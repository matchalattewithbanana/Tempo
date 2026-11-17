package me.avinas.tempo.service

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import me.avinas.tempo.data.local.entities.ListeningEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineQueueCompatibilityTest {
    private val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter<List<PendingEvent>>(
            Types.newParameterizedType(List::class.java, PendingEvent::class.java)
        )

    private val event = ListeningEvent(
        track_id = 14L,
        timestamp = 1_700_000_000_000L,
        playDuration = 95_000L,
        completionPercentage = 60,
        source = "com.spotify.music",
        tzOffsetMinutes = 0,
    )

    @Test fun `old durable events without retry deadline are immediately eligible`() {
        val pending = PendingEvent(event, "session-1", 1_700_000_001_000L, 3)
        val json = adapter.toJson(listOf(pending))
        // Earlier published queue snapshots never carried nextRetryAtMs.
        val legacy = json.replace(Regex(",?\\\"nextRetryAtMs\\\":0"), "")
        val parsed = requireNotNull(adapter.fromJson(legacy))
        assertEquals(1, parsed.size)
        assertEquals(0L, parsed.single().nextRetryAtMs)
        assertEquals(3, parsed.single().retryCount)
        assertEquals("session-1", parsed.single().sessionId)
    }

    @Test fun `future retry deadline survives a durable store round trip`() {
        val now = 1_700_000_002_000L
        val pending = PendingEvent(event, "session-2", now, 5, nextRetryAtMs = now + 60_000L)
        val restored = requireNotNull(adapter.fromJson(adapter.toJson(listOf(pending)))).single()
        assertTrue(restored.nextRetryAtMs > now)
        assertFalse(restored.nextRetryAtMs <= now)
        assertEquals(5, restored.retryCount)
    }
}
