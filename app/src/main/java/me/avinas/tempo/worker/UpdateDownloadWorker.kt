package me.avinas.tempo.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.avinas.tempo.R
import me.avinas.tempo.data.update.UpdateRepository

/** Survives screen/process death; restarts incomplete transfers safely. Never installs. */
@HiltWorker
class UpdateDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val updates: UpdateRepository,
) : CoroutineWorker(context, params) {
    companion object {
        const val WORK_NAME = "tempo_update_download"
        const val KEY_TAG = "tag"
        const val KEY_APK_NAME = "apk_name"
        const val KEY_URL = "apk_url"
        const val KEY_SIZE = "apk_size"
        const val KEY_SHA = "apk_sha256"
        const val KEY_PERCENT = "percent"
        const val KEY_ERROR = "error"
        private const val CHANNEL_ID = "tempo_update_downloads"
        private const val NOTIFICATION_ID = 7163
    }

    private fun foreground(percent: Int): ForegroundInfo {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_ID, context.getString(R.string.update_center_title),
            NotificationManager.IMPORTANCE_LOW,
        ))
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.update_downloading))
            .setContentText(context.getString(R.string.update_download_progress, percent))
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(UpdateCheckWorker.updateIntent(context))
            .build()
        return if (Build.VERSION.SDK_INT >= 29)
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(NOTIFICATION_ID, notification)
    }

    override suspend fun doWork(): Result {
        val tag = inputData.getString(KEY_TAG) ?: return Result.failure()
        val name = inputData.getString(KEY_APK_NAME) ?: return Result.failure()
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val size = inputData.getLong(KEY_SIZE, -1L)
        val sha = inputData.getString(KEY_SHA) ?: return Result.failure()
        return try {
            setForeground(foreground(0))
            var lastProgress = -5
            updates.download(tag, name, url, size, sha) { percent ->
                if (percent == 100 || percent >= lastProgress + 5) {
                    lastProgress = percent
                    setProgress(workDataOf(KEY_PERCENT to percent, KEY_TAG to tag))
                    if (percent != 100) setForeground(foreground(percent))
                }
            }
            Result.success(workDataOf(KEY_TAG to tag))
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Result.failure(workDataOf(KEY_ERROR to
                (failure.localizedMessage ?: context.getString(R.string.update_download_failed)).take(180)))
        }
    }
}
