package me.avinas.tempo.data.local

import java.util.TreeMap
import me.avinas.tempo.data.local.entities.ListeningEvent

/**
 * Pure in-memory policy for temporal listening-event reconciliation.
 *
 * Room owns fetching and committing rows. This object owns search windows,
 * candidate ordering and replacement decisions.
 */
internal object ListeningEventReconciliation {
    data class Slot(
        val play: ListeningImportPolicy.Play,
        val existingId: Long? = null,
        val trackId: Long,
    )

    class Candidates(
        private val equivalentByTrackId: Map<Long, List<Long>>,
    ) {
        private val byTimestamp = TreeMap<Long, MutableList<Slot>>()

        fun add(slot: Slot) {
            byTimestamp.getOrPut(slot.play.timestamp) { mutableListOf() }.add(slot)
        }

        fun remove(slot: Slot) {
            byTimestamp[slot.play.timestamp]?.remove(slot)
        }

        fun closest(trackId: Long, play: ListeningImportPolicy.Play): Slot? {
            val range = searchRange(play.timestamp, play.source)
            val compatibleIds = equivalentByTrackId[trackId] ?: listOf(trackId)
            var closest: Slot? = null
            var shortest = Long.MAX_VALUE
            for (slots in byTimestamp.subMap(range.first, true, range.last, true).values) {
                for (slot in slots) {
                    // Aliasing is not transitive: an anonymous title match must
                    // never bridge two conflicting provider IDs.
                    if (slot.trackId !in compatibleIds) continue
                    val distance = ListeningImportPolicy.matchDistance(slot.play, play) ?: continue
                    val previous = closest
                    if (
                        previous == null ||
                        distance < shortest ||
                        (
                            distance == shortest &&
                                slot.play.authority > previous.play.authority
                        ) ||
                        (
                            distance == shortest &&
                                slot.play.authority == previous.play.authority &&
                                (slot.existingId ?: Long.MAX_VALUE) <
                                (previous.existingId ?: Long.MAX_VALUE)
                        )
                    ) {
                        closest = slot
                        shortest = distance
                    }
                }
            }
            return closest
        }
    }

    fun shouldReplace(
        conflict: Slot,
        incoming: ListeningImportPolicy.Play,
        replaceExistingOnMatch: Boolean,
    ): Boolean =
        conflict.existingId != null && (
            incoming.authority > conflict.play.authority ||
                (
                    replaceExistingOnMatch &&
                        incoming.authority == conflict.play.authority &&
                        incoming.source == conflict.play.source
                )
            )

    data class Plan(
        val toInsert: List<ListeningEvent>,
        val toDelete: Set<Long>,
        val skipped: Int,
    )

    /**
     * Build the in-memory mutation plan after Room has loaded the bounded candidate rows.
     * The DAO remains responsible only for fetching and committing; ordering, conflict
     * selection and replacement semantics stay in this pure policy.
     */
    fun plan(
        incomingEvents: List<ListeningEvent>,
        existingSlots: List<Slot>,
        equivalentByTrackId: Map<Long, List<Long>>,
        replaceExistingOnMatch: Boolean,
    ): Plan {
        val candidates = Candidates(equivalentByTrackId)
        existingSlots.forEach(candidates::add)

        val toInsert = ArrayList<ListeningEvent>(incomingEvents.size)
        val toDelete = linkedSetOf<Long>()
        var skipped = 0
        val incoming =
            incomingEvents
                .map { it to ListeningImportPolicy.Play(it) }
                .sortedWith(
                    compareByDescending<Pair<ListeningEvent, ListeningImportPolicy.Play>> {
                        it.second.authority
                    }.thenBy { it.second.timestamp },
                )

        for ((event, play) in incoming) {
            val conflict = candidates.closest(event.track_id, play)
            if (conflict != null) {
                if (
                    !shouldReplace(
                        conflict = conflict,
                        incoming = play,
                        replaceExistingOnMatch = replaceExistingOnMatch,
                    )
                ) {
                    skipped++
                    continue
                }
                toDelete += requireNotNull(conflict.existingId)
                candidates.remove(conflict)
            }

            toInsert += event
            candidates.add(
                Slot(
                    play = play,
                    trackId = event.track_id,
                ),
            )
        }

        return Plan(
            toInsert = toInsert,
            toDelete = toDelete,
            skipped = skipped,
        )
    }

    fun windows(events: List<ListeningEvent>): List<LongRange> {
        val windows = ArrayList<LongRange>()
        for (range in events.map { searchRange(it.timestamp, it.source) }.sortedBy { it.first }) {
            val previous = windows.lastOrNull()
            if (
                previous != null &&
                range.first <= previous.last
                    .coerceAtMost(Long.MAX_VALUE - QUERY_JOIN_GAP_MS) + QUERY_JOIN_GAP_MS
            ) {
                windows[windows.lastIndex] = previous.first..maxOf(previous.last, range.last)
            } else {
                windows.add(range)
            }
        }
        return windows
    }

    private fun searchRange(timestamp: Long, source: String): LongRange {
        val padding = ListeningImportPolicy.searchPadding(source)
        return (timestamp.coerceAtLeast(Long.MIN_VALUE + padding) - padding)..
            (timestamp.coerceAtMost(Long.MAX_VALUE - padding) + padding)
    }

    private const val QUERY_JOIN_GAP_MS = 1_000L
}
