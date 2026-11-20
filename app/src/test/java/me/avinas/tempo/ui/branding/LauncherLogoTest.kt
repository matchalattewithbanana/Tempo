package me.avinas.tempo.ui.branding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherLogoTest {
    @Test fun allAliasesAreDistinctAndOwnedByTempo() {
        assertEquals(5, LauncherLogo.entries.size)
        assertEquals(5, LauncherLogo.entries.map { it.aliasClassName }.toSet().size)
        assertTrue(LauncherLogo.entries.all {
            it.aliasClassName.startsWith("me.avinas.tempo.launcher.")
        })
    }

    @Test fun originalIsThePersistentFallback() {
        assertEquals(LauncherLogo.ORIGINAL, LauncherLogo.entries.first())
        assertEquals("me.avinas.tempo.launcher.ClassicIcon", LauncherLogo.ORIGINAL.aliasClassName)
    }

    @Test fun launcherIconAssetsAreDistinct() {
        assertEquals(5, LauncherLogo.entries.map { it.imageRes }.toSet().size)
    }
}
