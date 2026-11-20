package me.avinas.tempo.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TempoAboutCreditsTest {
    @Test fun aboutListsOnlyTheCurrentMaintainerAndTheOriginalCreator() {
        assertEquals(
            listOf("matchalattewithbanana", "avinaxhroy"),
            TEMPO_ABOUT_CREDITS.map { it.handle },
        )
    }

    @Test fun creditsLinkToTheCorrectGithubAccounts() {
        assertEquals(
            TEMPO_ABOUT_CREDITS.map { "https://github.com/${it.handle}" },
            TEMPO_ABOUT_CREDITS.map { it.profileUrl },
        )
        assertEquals(2, TEMPO_ABOUT_CREDITS.map { it.profileUrl }.distinct().size)
        assertTrue(TEMPO_ABOUT_CREDITS.all { it.profileUrl.startsWith("https://github.com/") })
    }
}
