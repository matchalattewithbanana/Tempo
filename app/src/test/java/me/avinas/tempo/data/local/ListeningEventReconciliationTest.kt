package me.avinas.tempo.data.local

import me.avinas.tempo.data.local.entities.ListeningEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningEventReconciliationTest {
    private fun play(
        source: String,
        timestamp: Long = 1_700_000_000_000L,
        duration: Long = 120_000L,
    ) = ListeningImportPolicy.Play(
        timestamp = timestamp,
        endTimestamp = timestamp + duration,
        duration = duration,
        source = source,
        sessionId = null,
    )

    @Test fun closestCandidateRespectsDirectTrackCompatibility() {
        val candidates = ListeningEventReconciliation.Candidates(
            equivalentByTrackId = mapOf(10L to listOf(10L)),
        )
        candidates.add(
            ListeningEventReconciliation.Slot(
                play = play("com.spotify.music.import.json"),
                existingId = 7L,
                trackId = 20L,
            ),
        )

        assertNull(candidates.closest(10L, play("com.spotify.music")))
    }

    @Test fun higherAuthorityIncomingPlayCanReplaceImport() {
        val old = ListeningEventReconciliation.Slot(
            play = play("com.spotify.music.import.json"),
            existingId = 11L,
            trackId = 10L,
        )

        assertTrue(
            ListeningEventReconciliation.shouldReplace(
                conflict = old,
                incoming = play("com.spotify.music"),
                replaceExistingOnMatch = false,
            ),
        )
    }

    @Test fun sameAuthorityReplacementRequiresExplicitReplaceAndSameSource() {
        val old = ListeningEventReconciliation.Slot(
            play = play("com.spotify.music.import.json"),
            existingId = 11L,
            trackId = 10L,
        )
        val same = play("com.spotify.music.import.json")
        assertFalse(
            ListeningEventReconciliation.shouldReplace(
                conflict = old,
                incoming = same,
                replaceExistingOnMatch = false,
            ),
        )
        assertTrue(
            ListeningEventReconciliation.shouldReplace(
                conflict = old,
                incoming = same,
                replaceExistingOnMatch = true,
            ),
        )
    }

    @Test fun sparseEventsProduceSeparateDatabaseWindows() {
        val first = ListeningEvent(
            track_id = 1L,
            timestamp = 1_700_000_000_000L,
            playDuration = 120_000L,
                completionPercentage = 100,
            source = "com.spotify.music.import.json",
        )
        val second = first.copy(timestamp = first.timestamp + 86_400_000L)

        assertEquals(2, ListeningEventReconciliation.windows(listOf(first, second)).size)
    }

    @Test fun planReplacesLowerAuthorityExistingPlay() {
        val incoming =
            ListeningEvent(
                track_id = 10L,
                timestamp = 1_700_000_000_000L,
                playDuration = 120_000L,
                completionPercentage = 100,
                source = "com.spotify.music",
            )
        val existing =
            ListeningEventReconciliation.Slot(
                play = play("com.spotify.music.import.json"),
                existingId = 11L,
                trackId = 10L,
            )

        val plan =
            ListeningEventReconciliation.plan(
                incomingEvents = listOf(incoming),
                existingSlots = listOf(existing),
                equivalentByTrackId = mapOf(10L to listOf(10L)),
                replaceExistingOnMatch = false,
            )

        assertEquals(listOf(incoming), plan.toInsert)
        assertEquals(setOf(11L), plan.toDelete)
        assertEquals(0, plan.skipped)
    }

    @Test fun planKeepsHigherAuthorityExistingPlay() {
        val incoming =
            ListeningEvent(
                track_id = 10L,
                timestamp = 1_700_000_000_000L,
                playDuration = 120_000L,
                completionPercentage = 100,
                source = "com.spotify.music.import.json",
            )
        val existing =
            ListeningEventReconciliation.Slot(
                play = play("com.spotify.music"),
                existingId = 11L,
                trackId = 10L,
            )

        val plan =
            ListeningEventReconciliation.plan(
                incomingEvents = listOf(incoming),
                existingSlots = listOf(existing),
                equivalentByTrackId = mapOf(10L to listOf(10L)),
                replaceExistingOnMatch = true,
            )

        assertTrue(plan.toInsert.isEmpty())
        assertTrue(plan.toDelete.isEmpty())
        assertEquals(1, plan.skipped)
    }

    @Test fun planCollapsesEquivalentIncomingDuplicatesWithinBatch() {
        val first =
            ListeningEvent(
                track_id = 10L,
                timestamp = 1_700_000_000_000L,
                playDuration = 120_000L,
                completionPercentage = 100,
                source = "com.spotify.music.import.json",
            )
        val duplicate = first.copy(id = 99L)

        val plan =
            ListeningEventReconciliation.plan(
                incomingEvents = listOf(first, duplicate),
                existingSlots = emptyList(),
                equivalentByTrackId = mapOf(10L to listOf(10L)),
                replaceExistingOnMatch = false,
            )

        assertEquals(1, plan.toInsert.size)
        assertTrue(plan.toDelete.isEmpty())
        assertEquals(1, plan.skipped)
    }

}
