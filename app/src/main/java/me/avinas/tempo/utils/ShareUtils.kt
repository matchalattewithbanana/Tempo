package me.avinas.tempo.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.EntryPointAccessors
import me.avinas.tempo.data.analytics.FeatureFirstUsed
import me.avinas.tempo.data.analytics.TempoFeature
import me.avinas.tempo.di.AnalyticsEntryPoint
import java.io.File
import me.avinas.tempo.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShareUtils {

    /**
     * Resolves the analytics tracker without an injection point.
     *
     * Sharing happens inside plain composables and this stateless object, so there is nowhere
     * to take a constructor dependency. Failures are swallowed: a share must never break
     * because reporting could not be set up (for example under a unit test with no Hilt
     * application).
     *
     * Reported when the system share sheet opens, not when a target is chosen — Android does
     * not tell us which app the user picked, or whether they picked one at all.
     */
    private fun reportShare(context: Context, feature: TempoFeature) {
        runCatching {
            EntryPointAccessors
                .fromApplication(context.applicationContext, AnalyticsEntryPoint::class.java)
                .analyticsTracker()
                .track(FeatureFirstUsed(feature))
        }
    }

    suspend fun shareBitmap(
        context: Context,
        bitmap: Bitmap,
        feature: TempoFeature = TempoFeature.SHARE_CARD,
        launchChooser: ((Intent) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val directory = File(context.cacheDir, "images")
            if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) return@withContext false
            val now = System.currentTimeMillis()
            directory.listFiles()?.forEach { entry ->
                if (entry.name.startsWith("tempo-card-") && now - entry.lastModified() > SHARE_CACHE_AGE_MS) {
                    entry.delete()
                }
            }
            val file = File.createTempFile("tempo-card-", ".png", directory)
            try {
                val encoded = file.outputStream().buffered().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                if (!encoded) {
                    file.delete()
                    return@withContext false
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newUri(context.contentResolver, context.getString(R.string.share_image), uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                withContext(Dispatchers.Main) {
                    val chooser = Intent.createChooser(intent, context.getString(R.string.share_image))
                    if (launchChooser != null) launchChooser(chooser)
                    else context.startActivity(chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    reportShare(context, feature)
                }
                true
            } catch (cancelled: CancellationException) {
                // Cancellation after placing the chooser must leave its attachment readable.
                throw cancelled
            } catch (failure: Exception) {
                file.delete()
                throw failure
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            android.util.Log.w("ShareUtils", "Could not open image share", failure)
            false
        }
    }

    suspend fun saveBitmap(context: Context, bitmap: Bitmap, destination: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(destination, "wt")?.buffered()?.use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                } == true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                android.util.Log.w("ShareUtils", "Could not save share image", failure)
                false
            }
        }

    fun shareText(context: Context, text: String): Boolean = runCatching {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.share_as_text))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        reportShare(context, TempoFeature.SHARE_CARD)
    }.isSuccess

    fun copyText(context: Context, text: String): Boolean = runCatching {
        val clipboard = checkNotNull(context.getSystemService(ClipboardManager::class.java))
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.share_brand), text))
    }.isSuccess

    private const val SHARE_CACHE_AGE_MS = 24 * 60 * 60 * 1000L

    fun addBrandingToBitmap(original: Bitmap, brandingText: String = "Tempo"): Bitmap {
        val result = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 40f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
            // Add shadow for better visibility
            setShadowLayer(10f, 0f, 0f, android.graphics.Color.BLACK)
        }
        
        val padding = 40f
        canvas.drawText(brandingText, result.width - padding, result.height - padding, paint)
        return result
    }
}
