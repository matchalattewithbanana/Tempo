package me.avinas.tempo.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.avinas.tempo.MainActivity
import me.avinas.tempo.R
import me.avinas.tempo.data.update.UpdateRepository

/**
 * Opt-in only. Checks the public release manifest daily, never downloads or
 * installs updates without an explicit user action.
 */
@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val updates: UpdateRepository,
) : CoroutineWorker(context, params) {
    companion object {
        const val WORK_NAME = "tempo_update_check"
        const val CHANNEL_ID = "tempo_release_updates"
        private const val NOTIFICATION_ID = 7162
        fun updateIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .setAction("${context.packageName}.update_center")
                .putExtra("navigate_to", "update_center")
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            return PendingIntent.getActivity(context, 7162, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }

    override suspend fun doWork(): Result {
        if (!updates.automaticChecks) return Result.success()
        return try {
            val latest = updates.checkLatest()
            if (latest != null && updates.hasUpgrade(latest) &&
                !updates.isIgnored(latest.tag) && !updates.wasNotified(latest.tag) &&
                updates.automaticChecks
            ) {
                val canNotify = Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                if (canNotify) {
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.createNotificationChannel(NotificationChannel(
                        CHANNEL_ID, context.getString(R.string.update_center_title),
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply { description = context.getString(R.string.update_notifications_desc) })
                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(context.getString(R.string.update_notification_title, latest.tag))
                        .setContentText(context.getString(R.string.update_notification_body))
                        .setContentIntent(updateIntent(context))
                        .setAutoCancel(true)
                        .setOnlyAlertOnce(true)
                        .build()
                    manager.notify(NOTIFICATION_ID, notification)
                    updates.markNotified(latest.tag)
                }
            }
            Result.success()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
