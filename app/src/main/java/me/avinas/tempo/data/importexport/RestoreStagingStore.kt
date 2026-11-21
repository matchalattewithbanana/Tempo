package me.avinas.tempo.data.importexport

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.Closeable
import java.io.File
import me.avinas.tempo.data.local.entities.ListeningEvent
import me.avinas.tempo.data.local.entities.ScrobbleArchive

/**
 * Disk-backed staging for unbounded restore rows.
 *
 * Backup parsing writes typed rows here before the Room restore transaction starts. The Room
 * transaction then streams primitive SQLite rows instead of reparsing JSON while holding the
 * application's write lock. The store is disposable: committed app state never depends on it.
 */
internal class RestoreStagingStore(
    private val file: File,
) : Closeable {
    private val database: SQLiteDatabase

    init {
        SQLiteDatabase.deleteDatabase(file)
        database = SQLiteDatabase.openOrCreateDatabase(file, null)
        database.execSQL(
            """
            CREATE TABLE listening_events_stage(
                seq INTEGER PRIMARY KEY AUTOINCREMENT,
                track_id INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                play_duration INTEGER NOT NULL,
                completion_percentage INTEGER NOT NULL,
                source TEXT NOT NULL,
                was_skipped INTEGER NOT NULL,
                is_replay INTEGER NOT NULL,
                estimated_duration_ms INTEGER,
                pause_count INTEGER NOT NULL,
                session_id TEXT,
                end_timestamp INTEGER,
                total_pause_duration_ms INTEGER NOT NULL,
                seek_count INTEGER NOT NULL,
                position_updates_count INTEGER NOT NULL,
                was_interrupted INTEGER NOT NULL,
                volume_level INTEGER,
                tz_offset_minutes INTEGER
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE scrobble_archive_stage(
                seq INTEGER PRIMARY KEY AUTOINCREMENT,
                track_hash TEXT NOT NULL,
                track_title TEXT NOT NULL,
                artist_name TEXT NOT NULL,
                artist_name_normalized TEXT NOT NULL,
                album_name TEXT,
                musicbrainz_id TEXT,
                timestamps_blob BLOB NOT NULL,
                play_count INTEGER NOT NULL,
                first_scrobble INTEGER NOT NULL,
                last_scrobble INTEGER NOT NULL,
                album_art_url TEXT,
                was_loved INTEGER NOT NULL,
                import_id INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    fun <T> writeBatch(block: () -> T): T {
        database.beginTransaction()
        return try {
            val result = block()
            database.setTransactionSuccessful()
            result
        } finally {
            database.endTransaction()
        }
    }

    fun stage(event: ListeningEvent) {
        val values =
            ContentValues(18).apply {
                put("track_id", event.track_id)
                put("timestamp", event.timestamp)
                put("play_duration", event.playDuration)
                put("completion_percentage", event.completionPercentage)
                put("source", event.source)
                put("was_skipped", event.wasSkipped.asSqlInt())
                put("is_replay", event.isReplay.asSqlInt())
                putNullableLong("estimated_duration_ms", event.estimatedDurationMs)
                put("pause_count", event.pauseCount)
                put("session_id", event.sessionId)
                putNullableLong("end_timestamp", event.endTimestamp)
                put("total_pause_duration_ms", event.totalPauseDurationMs)
                put("seek_count", event.seekCount)
                put("position_updates_count", event.positionUpdatesCount)
                put("was_interrupted", event.wasInterrupted.asSqlInt())
                putNullableInt("volume_level", event.volumeLevel)
                putNullableInt("tz_offset_minutes", event.tzOffsetMinutes)
            }
        check(
            database.insertOrThrow("listening_events_stage", null, values) != -1L,
        ) { "Unable to stage listening event" }
    }

    fun stage(row: ScrobbleArchive) {
        val values =
            ContentValues(15).apply {
                put("track_hash", row.trackHash)
                put("track_title", row.trackTitle)
                put("artist_name", row.artistName)
                put("artist_name_normalized", row.artistNameNormalized)
                put("album_name", row.albumName)
                put("musicbrainz_id", row.musicbrainzId)
                put("timestamps_blob", row.timestampsBlob)
                put("play_count", row.playCount)
                put("first_scrobble", row.firstScrobble)
                put("last_scrobble", row.lastScrobble)
                put("album_art_url", row.albumArtUrl)
                put("was_loved", row.wasLoved.asSqlInt())
                put("import_id", row.importId)
                put("created_at", row.createdAt)
                put("updated_at", row.updatedAt)
            }
        check(
            database.insertOrThrow("scrobble_archive_stage", null, values) != -1L,
        ) { "Unable to stage scrobble archive row" }
    }

    suspend fun forEachEvent(block: suspend (ListeningEvent) -> Unit) {
        val cursor =
            database.query(
                "listening_events_stage",
                EVENT_COLUMNS,
                null,
                null,
                null,
                null,
                "seq ASC",
            )
        try {
            while (cursor.moveToNext()) {
                block(
                    ListeningEvent(
                        track_id = cursor.getLong(0),
                        timestamp = cursor.getLong(1),
                        playDuration = cursor.getLong(2),
                        completionPercentage = cursor.getInt(3),
                        source = cursor.getString(4),
                        wasSkipped = cursor.getInt(5) != 0,
                        isReplay = cursor.getInt(6) != 0,
                        estimatedDurationMs = cursor.longOrNull(7),
                        pauseCount = cursor.getInt(8),
                        sessionId = cursor.stringOrNull(9),
                        endTimestamp = cursor.longOrNull(10),
                        totalPauseDurationMs = cursor.getLong(11),
                        seekCount = cursor.getInt(12),
                        positionUpdatesCount = cursor.getInt(13),
                        wasInterrupted = cursor.getInt(14) != 0,
                        volumeLevel = cursor.intOrNull(15),
                        contentFingerprint = null,
                        tzOffsetMinutes = cursor.intOrNull(16),
                    ),
                )
            }
        } finally {
            cursor.close()
        }
    }

    suspend fun forEachArchiveRow(block: suspend (ScrobbleArchive) -> Unit) {
        val cursor =
            database.query(
                "scrobble_archive_stage",
                ARCHIVE_COLUMNS,
                null,
                null,
                null,
                null,
                "seq ASC",
            )
        try {
            while (cursor.moveToNext()) {
                block(
                    ScrobbleArchive(
                        trackHash = cursor.getString(0),
                        trackTitle = cursor.getString(1),
                        artistName = cursor.getString(2),
                        artistNameNormalized = cursor.getString(3),
                        albumName = cursor.stringOrNull(4),
                        musicbrainzId = cursor.stringOrNull(5),
                        timestampsBlob = cursor.getBlob(6),
                        playCount = cursor.getInt(7),
                        firstScrobble = cursor.getLong(8),
                        lastScrobble = cursor.getLong(9),
                        albumArtUrl = cursor.stringOrNull(10),
                        wasLoved = cursor.getInt(11) != 0,
                        importId = cursor.getLong(12),
                        createdAt = cursor.getLong(13),
                        updatedAt = cursor.getLong(14),
                    ),
                )
            }
        } finally {
            cursor.close()
        }
    }

    override fun close() {
        if (database.isOpen) database.close()
    }

    fun closeAndDelete() {
        close()
        SQLiteDatabase.deleteDatabase(file)
    }

    private fun Boolean.asSqlInt(): Int = if (this) 1 else 0

    private fun ContentValues.putNullableLong(key: String, value: Long?) {
        if (value == null) putNull(key) else put(key, value)
    }

    private fun ContentValues.putNullableInt(key: String, value: Int?) {
        if (value == null) putNull(key) else put(key, value)
    }

    private fun Cursor.longOrNull(index: Int): Long? =
        if (isNull(index)) null else getLong(index)

    private fun Cursor.intOrNull(index: Int): Int? =
        if (isNull(index)) null else getInt(index)

    private fun Cursor.stringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)

    private companion object {
        val EVENT_COLUMNS =
            arrayOf(
                "track_id",
                "timestamp",
                "play_duration",
                "completion_percentage",
                "source",
                "was_skipped",
                "is_replay",
                "estimated_duration_ms",
                "pause_count",
                "session_id",
                "end_timestamp",
                "total_pause_duration_ms",
                "seek_count",
                "position_updates_count",
                "was_interrupted",
                "volume_level",
                "tz_offset_minutes",
            )

        val ARCHIVE_COLUMNS =
            arrayOf(
                "track_hash",
                "track_title",
                "artist_name",
                "artist_name_normalized",
                "album_name",
                "musicbrainz_id",
                "timestamps_blob",
                "play_count",
                "first_scrobble",
                "last_scrobble",
                "album_art_url",
                "was_loved",
                "import_id",
                "created_at",
                "updated_at",
            )
    }
}
