package me.avinas.tempo.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TempoVersionTest {
    @Test fun stableIsNewerThanBetaOfTheSameVersion() {
        val beta = TempoVersion.parse("v4.9.10-beta.3")!!
        val stable = TempoVersion.parse("v4.9.10")!!
        assertTrue(stable > beta)
        assertTrue(beta > TempoVersion.parse("4.9.9")!!)
    }

    @Test fun nextBetaUsesNumericPatchOrderAndStaysBelowItsStableVersion() {
        val currentStable = TempoVersion.parse("v4.9.9")!!
        val candidate = TempoVersion.parse("v4.9.125-beta.1")!!
        val matchingStable = TempoVersion.parse("v4.9.125")!!
        assertTrue(candidate > currentStable)
        assertTrue(matchingStable > candidate)
        assertEquals(1, candidate.beta)
    }

    @Test fun numericComponentsAreNotComparedLexicographically() {
        assertTrue(TempoVersion.parse("4.10.0")!! > TempoVersion.parse("4.9.99")!!)
        assertTrue(TempoVersion.parse("v5.0.0")!! > TempoVersion.parse("4.99.99")!!)
        assertTrue(TempoVersion.parse("v4.9.10-beta.12")!! > TempoVersion.parse("v4.9.10-beta.9")!!)
    }

    @Test fun olderForkInstallsStillFindNewerReleases() {
        assertTrue(TempoVersion.parse("v4.9.0")!! > TempoVersion.parse("4.8.16-fork.2-beta.1")!!)
        assertTrue(TempoVersion.parse("4.8.16")!! > TempoVersion.parse("4.8.16-fork.1")!!)
    }

    @Test fun malformedOrUnrelatedReleaseNamesAreRejected() {
        assertNull(TempoVersion.parse("nightly-latest"))
        assertNull(TempoVersion.parse("4.9.8-custom.apk"))
        assertNull(TempoVersion.parse("v4.9"))
        assertNull(TempoVersion.parse("v4.9.8-beta.-1"))
    }

    @Test fun channelIdentifiersAreIndependent() {
        assertEquals(UpdateChannel.STABLE, UpdateChannel.valueOf("STABLE"))
        assertFalse(UpdateChannel.STABLE == UpdateChannel.PREVIEW)
    }
}
