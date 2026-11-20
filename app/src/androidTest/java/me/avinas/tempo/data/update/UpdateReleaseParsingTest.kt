package me.avinas.tempo.data.update

import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateReleaseParsingTest {
    private val repository by lazy {
        UpdateRepository(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    private fun release(
        tag: String = "v4.9.10",
        prerelease: Boolean = false,
        apkUrl: String = "https://github.com/matchalattewithbanana/Tempo/releases/download/v4.9.10/Tempo-4.9.10.apk",
        digest: String = "sha256:" + "ab".repeat(32),
    ): JSONObject = JSONObject().apply {
        put("tag_name", tag)
        put("name", "Tempo $tag")
        put("draft", false)
        put("prerelease", prerelease)
        put("body", "Release notes")
        put("html_url", "https://github.com/matchalattewithbanana/Tempo/releases/tag/$tag")
        put("assets", JSONArray().put(JSONObject().apply {
            put("name", "Tempo-${tag.removePrefix("v")}.apk")
            put("state", "uploaded")
            put("browser_download_url", apkUrl)
            put("size", 44_000_000)
            put("digest", digest)
        }))
    }

    @Test fun stableChannelAcceptsOnlyPublishedStableApks() {
        assertNotNull(repository.parseRelease(release(), UpdateChannel.STABLE))
        assertNull(repository.parseRelease(release(
            tag = "v4.9.10-beta.1", prerelease = true,
            apkUrl = "https://github.com/matchalattewithbanana/Tempo/releases/download/v4.9.10-beta.1/Tempo-4.9.10-beta.1.apk",
        ), UpdateChannel.STABLE))
    }

    @Test fun previewChannelIncludesPublishedBetaBuilds() {
        val candidate = repository.parseRelease(release(
            tag = "v4.9.10-beta.1", prerelease = true,
            apkUrl = "https://github.com/matchalattewithbanana/Tempo/releases/download/v4.9.10-beta.1/Tempo-4.9.10-beta.1.apk",
        ), UpdateChannel.PREVIEW)
        assertEquals("v4.9.10-beta.1", candidate?.tag)
    }

    @Test fun maliciousLinksAndMissingDigestsAreNeverInstallerCandidates() {
        assertNull(repository.parseRelease(release(
            apkUrl = "https://example.org/Tempo-4.9.10.apk",
        ), UpdateChannel.PREVIEW))
        assertNull(repository.parseRelease(release(digest = ""), UpdateChannel.PREVIEW))
        assertNull(repository.parseRelease(release(digest = "sha256:garbage"), UpdateChannel.PREVIEW))
    }

    @Test fun nonReleaseLegacyTagsCannotBeSelectedForAutomaticDownloads() {
        assertNull(repository.parseRelease(release(
            tag = "v4.8.16-fork.1",
            apkUrl = "https://github.com/matchalattewithbanana/Tempo/releases/download/v4.8.16-fork.1/Tempo-4.8.16-fork.1.apk",
        ), UpdateChannel.PREVIEW))
    }
}
