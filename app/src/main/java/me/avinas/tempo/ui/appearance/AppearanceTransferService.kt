package me.avinas.tempo.ui.appearance

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Owns bounded JSON file I/O for Appearance Studio import/export. */
@Singleton
class AppearanceTransferService @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun read(uri: Uri): AppearanceSettings = withContext(Dispatchers.IO) {
        requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
            val bytes = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(bytes.size() + count <= AppearanceCodec.MAX_BYTES) {
                    "Appearance file is too large"
                }
                bytes.write(buffer, 0, count)
            }
            AppearanceCodec.decode(bytes.toString(Charsets.UTF_8.name())).forLiveApp()
        }
    }

    suspend fun write(uri: Uri, settings: AppearanceSettings) = withContext(Dispatchers.IO) {
        val payload = AppearanceCodec.encode(settings)
        require(payload.toByteArray(Charsets.UTF_8).size <= AppearanceCodec.MAX_BYTES) {
            "Appearance file is too large"
        }
        requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { output ->
            output.write(payload.toByteArray(Charsets.UTF_8))
        }
    }
}
