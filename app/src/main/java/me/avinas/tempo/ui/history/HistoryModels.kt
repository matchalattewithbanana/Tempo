package me.avinas.tempo.ui.history

import androidx.compose.runtime.Immutable
import me.avinas.tempo.data.local.dao.HistoryItem

/**
 * History view mode - controls what data is shown.
 */
enum class HistoryViewMode {
    /**
     * Unified mode: Shows all listening events combined chronologically.
     * This is the default when no Last.fm import exists.
     */
    UNIFIED,
    
    /**
     * Separated mode: Shows two sections:
     * 1. "Recent Activity" - Live tracking (Spotify/notifications, source != fm.last.import)
     * 2. "Last.fm History" - Imported history (source == fm.last.import + archive)
     * This is the default when Last.fm import exists.
     */
    SEPARATED
}

/**
 * Represents an archived scrobble expanded for display in history.
 * Archive stores compressed timestamps, so we expand them for timeline display.
 */
data class ArchiveHistoryItem(
    val archiveId: Long,
    val timestamp: Long,
    val trackTitle: String,
    val artistName: String,
    val albumName: String?,
    val albumArtUrl: String?,
    val playCount: Int, // Total plays of this track in archive
    val isFromArchive: Boolean = true
)


@Immutable
data class HistoryUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val isMarking: Boolean = false, // True when marking content in progress
    val loadFailed: Boolean = false,
    val pageFailed: Boolean = false,
    val lastFmFailedPage: Int? = null,
    val mutationError: Int? = null,
    
    // Recent Activity Section (non-Last.fm events)
    val rawItems: List<HistoryItem> = emptyList(),
    val groupedItems: Map<String, List<HistoryItem>> = emptyMap(),
    val page: Int = 0,
    val hasMore: Boolean = true,
    
    // Last.fm History Section (imported events)
    val lastFmItems: List<HistoryItem> = emptyList(),
    val lastFmGroupedItems: Map<String, List<HistoryItem>> = emptyMap(),
    val lastFmPage: Int = 0,
    val hasMoreLastFm: Boolean = true,
    val isLoadingMoreLastFm: Boolean = false,
    
    // Filters
    val searchQuery: String = "",
    val searchPending: Boolean = false,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val showSkips: Boolean = true,
    val showCoachMark: Boolean = false,
    // User feedback
    val feedbackMessage: String? = null,
    
    // View Mode & Archive Integration
    
    /**
     * Current view mode:
     * - UNIFIED: All events combined (no Last.fm import)
     * - SEPARATED: Two sections - Recent Activity + Last.fm History
     */
    val viewMode: HistoryViewMode = HistoryViewMode.UNIFIED,
    
    /**
     * Whether user has archived Last.fm scrobbles.
     */
    val hasArchiveData: Boolean = false,
    
    /**
     * Number of unique tracks in archive.
     */
    val archiveTrackCount: Int = 0,
    
    /**
     * Total play count across all archived tracks.
     */
    val archiveTotalPlays: Long = 0,
    
    /**
     * Timestamp of the Last.fm import (for UI display).
     */
    val recentBoundaryDate: Long? = null,
    
    /**
     * Archived items (long-tail tracks from Last.fm import).
     * Shown at the bottom of Last.fm History section.
     */
    val archiveItems: List<ArchiveHistoryItem> = emptyList()
)

