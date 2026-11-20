package me.avinas.tempo.ui.updates

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.avinas.tempo.data.update.TempoRelease
import me.avinas.tempo.data.update.UpdateChannel
import me.avinas.tempo.data.update.UpdateRepository
import me.avinas.tempo.worker.UpdateDownloadWorker
import javax.inject.Inject

data class UpdateCenterState(
    val installedVersion: String,
    val channel: UpdateChannel,
    val autoCheck: Boolean,
    val lastCheckedAt: Long,
    val checking: Boolean = false,
    val release: TempoRelease? = null,
    val newerAvailable: Boolean = false,
    val ignored: Boolean = false,
    val downloading: Boolean = false,
    val percent: Int = 0,
    val downloaded: Boolean = false,
    val installing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class UpdateCenterViewModel @Inject constructor(
    private val repository: UpdateRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val manager = WorkManager.getInstance(context)
    private val _state = MutableStateFlow(UpdateCenterState(
        installedVersion = repository.installedVersion, channel = repository.channel,
        autoCheck = repository.automaticChecks, lastCheckedAt = repository.lastCheckedAt,
    ))
    val state: StateFlow<UpdateCenterState> = _state.asStateFlow()
    private var checkJob: Job? = null

    init {
        viewModelScope.launch {
            manager.getWorkInfosForUniqueWorkFlow(UpdateDownloadWorker.WORK_NAME).collect { jobs ->
                // Unique work chains can contain an older cancelled run. Prefer
                // the current active request so progress never jumps backwards.
                val job = jobs.firstOrNull { it.state == WorkInfo.State.RUNNING }
                    ?: jobs.firstOrNull { it.state == WorkInfo.State.ENQUEUED }
                    ?: jobs.lastOrNull()
                _state.update { previous ->
                    val release = previous.release
                    val matches = release != null && (
                        job?.progress?.getString(UpdateDownloadWorker.KEY_TAG) == release.tag ||
                            job?.outputData?.getString(UpdateDownloadWorker.KEY_TAG) == release.tag
                        )
                    val active = matches && job?.state in listOf(
                        WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED,
                    )
                    val ready = release != null && repository.downloadedFile(release.tag)?.exists() == true
                    val failure = if (matches && job?.state == WorkInfo.State.FAILED)
                        job.outputData.getString(UpdateDownloadWorker.KEY_ERROR) ?: "Download failed"
                        else null
                    previous.copy(
                        downloading = active,
                        percent = if (active) job?.progress?.getInt(UpdateDownloadWorker.KEY_PERCENT, 0) ?: 0 else 0,
                        downloaded = ready && previous.newerAvailable,
                        error = failure ?: previous.error,
                    )
                }
            }
        }
        refresh()
    }

    fun refresh() {
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            _state.update { it.copy(checking = true, error = null, release = null,
                newerAvailable = false, downloaded = false) }
            try {
                val channel = repository.channel
                val latest = repository.checkLatest(channel)
                // A channel switch cancels the previous request; ignore any late result.
                if (channel != repository.channel) return@launch
                _state.update {
                    it.copy(checking = false, release = latest,
                        newerAvailable = latest != null && repository.hasUpgrade(latest),
                        ignored = latest != null && repository.ignoredTag == latest.tag,
                        downloaded = latest != null && repository.hasUpgrade(latest) &&
                            repository.downloadedFile(latest.tag)?.exists() == true,
                        lastCheckedAt = repository.lastCheckedAt)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _state.update { it.copy(checking = false,
                    error = failure.localizedMessage ?: "Could not reach the release server") }
            }
        }
    }

    fun selectChannel(channel: UpdateChannel) {
        if (repository.channel == channel) return
        repository.setChannel(channel)
        _state.update { it.copy(channel = channel, ignored = false) }
        refresh()
    }

    fun setAutomaticChecks(enabled: Boolean) {
        repository.setAutomaticChecks(enabled)
        _state.update { it.copy(autoCheck = repository.automaticChecks) }
    }

    fun ignoreThisRelease() {
        val release = _state.value.release ?: return
        repository.ignore(release.tag)
        _state.update { it.copy(ignored = true) }
    }

    fun startDownload() {
        val state = _state.value
        val release = state.release ?: return
        if (!state.newerAvailable || state.downloading || state.downloaded) return
        val request = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
            .setInputData(workDataOf(
                UpdateDownloadWorker.KEY_TAG to release.tag,
                UpdateDownloadWorker.KEY_APK_NAME to release.apkName,
                UpdateDownloadWorker.KEY_URL to release.apkUrl,
                UpdateDownloadWorker.KEY_SIZE to release.apkBytes,
                UpdateDownloadWorker.KEY_SHA to release.sha256,
            ))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        manager.enqueueUniqueWork(UpdateDownloadWorker.WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        _state.update { it.copy(downloading = true, percent = 0, error = null) }
    }

    fun cancelDownload() {
        manager.cancelUniqueWork(UpdateDownloadWorker.WORK_NAME)
        _state.update { it.copy(downloading = false) }
    }

    fun install() {
        val release = _state.value.release ?: return
        if (!_state.value.newerAvailable || !_state.value.downloaded) return
        viewModelScope.launch {
            _state.update { it.copy(installing = true, error = null) }
            try {
                val apk = repository.verifyInstalledPackageCandidate(release)
                if (!repository.canInstallPackages()) repository.requestInstallPermission()
                else repository.openSystemInstaller(apk)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _state.update { it.copy(error = failure.localizedMessage ?: "Could not open Android installer") }
            } finally {
                _state.update { it.copy(installing = false) }
            }
        }
    }

    fun openReleasePage(): String? = _state.value.release?.pageUrl
}
