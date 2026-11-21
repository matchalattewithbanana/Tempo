package me.avinas.tempo.data.importexport

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import me.avinas.tempo.data.profile.ProfileIdentityManager
import me.avinas.tempo.data.repository.StatsRepository
import me.avinas.tempo.ui.appearance.AppearancePreferencesRepository
import me.avinas.tempo.utils.ImageUrlHostAllowlist
import me.avinas.tempo.worker.PostRestoreCacheWorker

/**
 * Owns the non-Room phase after a backup transaction commits.
 *
 * Profile identity and Appearance Studio use separate persistence systems, so they cannot be
 * atomically committed with Room. This coordinator makes that boundary explicit: the library
 * restore is already durable, post-commit failures are reported as warnings, and cache work is
 * derived only from allow-listed URLs.
 */
@Singleton
class PostRestoreCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val profileIdentityManager: ProfileIdentityManager,
    private val appearanceRepository: AppearancePreferencesRepository,
    private val statsRepository: StatsRepository,
) {
    data class Outcome(
        val appearanceStatus: BackupAppearanceStatus,
        val warnings: List<BackupRestoreWarning>,
    )

    suspend fun complete(
        data: TempoExportData,
        pathMapping: Map<String, String>,
        conflictStrategy: ImportConflictStrategy,
        onProgress: (ImportExportProgress) -> Unit,
    ): Outcome {
        val warnings = mutableListOf<String>()

        if (conflictStrategy == ImportConflictStrategy.REPLACE) {
            try {
                data.userName?.takeIf { it.isNotBlank() }?.let(profileIdentityManager::updateUserName)
                profileIdentityManager.restoreProfileImagePath(
                    resolveRestoredProfileImagePath(
                        exportedProfileImagePath = data.userProfileImagePath,
                        pathMapping = pathMapping,
                    ),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Log.w(TAG, "Library restored but profile identity could not be restored", failure)
                warnings += BackupRestoreWarning.PROFILE_IDENTITY
            }
        }

        val appearanceRestored =
            try {
                data.appearance?.let {
                    appearanceRepository.restoreBackup(
                        it,
                        replace = conflictStrategy == ImportConflictStrategy.REPLACE,
                    )
                } ?: false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Log.w(TAG, "Library restored but appearance settings could not be written", failure)
                warnings += BackupRestoreWarning.APPEARANCE
                false
            }

        val cacheableHotlinks = ImageUrlHostAllowlist.filterAllowed(data.hotlinkedUrls)
        val droppedHotlinks = data.hotlinkedUrls.size - cacheableHotlinks.size
        if (droppedHotlinks > 0) {
            Log.w(
                TAG,
                "Dropped $droppedHotlinks non-allowlisted hotlinked URLs from restored backup",
            )
        }
        if (cacheableHotlinks.isNotEmpty()) {
            try {
                onProgress(ImportExportProgress("Scheduling image cache...", 95, 100))
                PostRestoreCacheWorker.schedule(context, cacheableHotlinks)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Log.w(TAG, "Library restored but image pre-cache could not be scheduled", failure)
                warnings += BackupRestoreWarning.IMAGE_CACHE
            }
        }

        // Force Home/Stats to observe committed data rather than a pre-restore cache snapshot.
        statsRepository.invalidateCache()

        return Outcome(
            appearanceStatus =
                if (appearanceRestored) {
                    BackupAppearanceStatus.RESTORED
                } else {
                    BackupAppearanceStatus.UNCHANGED
                },
            warnings = warnings.toList(),
        )
    }

    private companion object {
        const val TAG = "PostRestoreCoordinator"
    }
}
