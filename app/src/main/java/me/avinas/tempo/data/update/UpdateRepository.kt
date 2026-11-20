package me.avinas.tempo.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import me.avinas.tempo.BuildConfig
import me.avinas.tempo.worker.UpdateCheckWorker
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import javax.net.ssl.HttpsURLConnection

@Singleton
class UpdateRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs = context.getSharedPreferences("tempo_updates", Context.MODE_PRIVATE)

    val installedVersion: String get() = BuildConfig.VERSION_NAME
    val currentVersion: TempoVersion? get() = TempoVersion.parse(installedVersion)
    val channel: UpdateChannel get() = runCatching {
        UpdateChannel.valueOf(prefs.getString("channel", "STABLE") ?: "STABLE")
    }.getOrDefault(UpdateChannel.STABLE)
    val automaticChecks: Boolean get() = prefs.getBoolean("auto_checks", false)
    fun isIgnored(tag: String): Boolean = tag in (prefs.getStringSet("ignored_tags", emptySet()) ?: emptySet())
        || prefs.getString("ignored_tag", null) == tag
    val lastCheckedAt: Long get() = prefs.getLong("last_checked", 0L)

    fun setChannel(channel: UpdateChannel) {
        // Muting a release survives channel changes.
        prefs.edit().putString("channel", channel.name).apply()
    }

    fun setAutomaticChecks(enabled: Boolean) {
        prefs.edit().putBoolean("auto_checks", enabled).apply()
        val manager = WorkManager.getInstance(context)
        if (!enabled) {
            manager.cancelUniqueWork(UpdateCheckWorker.WORK_NAME)
        } else {
            val work = PeriodicWorkRequestBuilder<UpdateCheckWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(24, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            manager.enqueueUniquePeriodicWork(
                UpdateCheckWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, work,
            )
        }
    }

    fun ignore(tag: String) {
        val prior = prefs.getStringSet("ignored_tags", emptySet()).orEmpty().toMutableSet()
        prior.add(tag)
        prefs.edit().putStringSet("ignored_tags", prior).apply()
    }
    fun wasNotified(tag: String): Boolean =
        tag in prefs.getStringSet("notified_tags", emptySet()).orEmpty() ||
            prefs.getString("notified_tag", null) == tag
    fun markNotified(tag: String) {
        val prior = prefs.getStringSet("notified_tags", emptySet()).orEmpty().toMutableSet()
        prior.add(tag)
        prefs.edit().putStringSet("notified_tags", prior).apply()
    }

    suspend fun checkLatest(channel: UpdateChannel = this.channel): TempoRelease? = withContext(Dispatchers.IO) {
        val json = readHttpsText("https://api.github.com/repos/matchalattewithbanana/Tempo/releases?per_page=40", 1_048_576)
        val releases = JSONArray(json)
        val candidates = (0 until releases.length()).mapNotNull { index ->
            parseRelease(releases.optJSONObject(index), channel)
        }
        prefs.edit().putLong("last_checked", System.currentTimeMillis()).apply()
        candidates.maxByOrNull { it.version }
    }

    fun hasUpgrade(release: TempoRelease): Boolean =
        currentVersion?.let { release.version > it } == true

    /** GitHub is the sole update authority. Never accept an arbitrary APK link. */
    internal fun isReleaseUrl(tag: String, apkName: String, url: String): Boolean =
        tag.matches(Regex("""v\d+\.\d+\.\d+(?:-beta\.\d+)?""")) &&
            apkName == "Tempo-${tag.removePrefix("v")}.apk" &&
            url == "https://github.com/matchalattewithbanana/Tempo/releases/download/$tag/$apkName"

    internal fun parseRelease(json: org.json.JSONObject?, channel: UpdateChannel): TempoRelease? {
        if (json == null || json.optBoolean("draft")) return null
        val tag = json.optString("tag_name")
        val version = TempoVersion.parse(tag) ?: return null
        val prerelease = json.optBoolean("prerelease")
        if (channel == UpdateChannel.STABLE && (prerelease || version.beta != null)) return null
        if (version.beta != null && !prerelease) return null
        val apkName = "Tempo-${tag.removePrefix("v")}.apk"
        val assets = json.optJSONArray("assets") ?: return null
        val apk = (0 until assets.length()).mapNotNull(assets::optJSONObject)
            .firstOrNull { it.optString("name") == apkName && it.optString("state") == "uploaded" } ?: return null
        val url = apk.optString("browser_download_url")
        val size = apk.optLong("size", -1)
        val digest = apk.optString("digest").removePrefix("sha256:").lowercase()
        if (!isReleaseUrl(tag, apkName, url) || size !in 1_000_000L..250_000_000L ||
            !digest.matches(Regex("""[a-f0-9]{64}"""))) return null
        val pageUrl = json.optString("html_url")
        if (pageUrl != "https://github.com/matchalattewithbanana/Tempo/releases/tag/$tag") return null
        return TempoRelease(
            tag, version, json.optString("name").take(160),
            json.optString("body").take(16_000), json.optString("published_at"),
            prerelease, pageUrl, url, apkName, size, digest,
        )
    }

    private fun readHttpsText(url: String, limit: Int): String {
        val connection = openTrustedConnection(url, allowAssetRedirect = false)
        try {
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Tempo-Updater/${BuildConfig.VERSION_NAME}")
            if (connection.responseCode != 200) throw IOException("Release service HTTP ${connection.responseCode}")
            return connection.inputStream.use { stream ->
                val buffer = ByteArray(8192)
                val output = java.io.ByteArrayOutputStream()
                while (true) {
                    val n = stream.read(buffer)
                    if (n < 0) break
                    if (output.size() + n > limit) throw IOException("Release metadata is too large")
                    output.write(buffer, 0, n)
                }
                output.toString(Charsets.UTF_8.name())
            }
        } finally { connection.disconnect() }
    }

    private fun openTrustedConnection(url: String, allowAssetRedirect: Boolean): HttpsURLConnection {
        val parsed = URL(url)
        if (parsed.protocol != "https" || parsed.port != -1 ||
            parsed.userInfo != null || parsed.ref != null) throw IOException("Unsafe update URL")
        val trusted = if (allowAssetRedirect)
            setOf("github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com")
            else setOf("api.github.com")
        if (parsed.host !in trusted) throw IOException("Untrusted update host")
        return (parsed.openConnection() as HttpsURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 25_000
            instanceFollowRedirects = false
        }
    }

    private fun openApk(url: String): HttpsURLConnection {
        var current = url
        repeat(6) {
            val connection = openTrustedConnection(current, allowAssetRedirect = true)
            connection.setRequestProperty("User-Agent", "Tempo-Updater/${BuildConfig.VERSION_NAME}")
            val code = connection.responseCode
            if (code == 200) return connection
            val next = connection.getHeaderField("Location")
            connection.disconnect()
            if (code !in listOf(301, 302, 303, 307, 308) || next.isNullOrEmpty())
                throw IOException("APK download HTTP $code")
            // Resolve relative redirects while preserving the HTTPS allowlist.
            current = URL(URL(current), next).toString()
        }
        throw IOException("Too many release redirects")
    }

    fun downloadedFile(tag: String): File? {
        if (!tag.matches(Regex("""v\d+\.\d+\.\d+(?:-beta\.\d+)?"""))) return null
        val name = "Tempo-${tag.removePrefix("v")}.apk"
        if (!isReleaseUrl(tag, name,
                "https://github.com/matchalattewithbanana/Tempo/releases/download/$tag/$name")) return null
        return File(File(context.filesDir, "updates"), name)
    }

    suspend fun download(
        tag: String, apkName: String, url: String, expectedBytes: Long, expectedSha: String,
        onProgress: suspend (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        if (!isReleaseUrl(tag, apkName, url) || expectedBytes !in 1_000_000L..250_000_000L ||
            !expectedSha.matches(Regex("""[a-f0-9]{64}""")))
            throw IOException("Invalid release metadata")
        val dir = File(context.filesDir, "updates")
        if (!dir.exists() && !dir.mkdirs()) throw IOException("Could not prepare update storage")
        val target = File(dir, apkName)
        if (target.exists()) {
            if (target.length() == expectedBytes && sha256(target) == expectedSha) return@withContext target
            target.delete()
        }
        // Unique per WorkManager attempt: cancellation cannot delete the next
        // request's partial file when the user changes update channels.
        dir.listFiles()?.filter { it.name.endsWith(".part") &&
            System.currentTimeMillis() - it.lastModified() > 86_400_000L
        }?.forEach { it.delete() }
        val partial = File(dir, "$apkName.${java.util.UUID.randomUUID()}.part")
        var downloaded = 0L
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            val connection = openApk(url)
            try {
                connection.inputStream.use { input ->
                    partial.outputStream().buffered().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            downloaded += read
                            if (downloaded > expectedBytes) throw IOException("Update exceeds expected size")
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                            onProgress((downloaded * 100L / expectedBytes).toInt().coerceIn(0, 99))
                        }
                    }
                }
            } finally { connection.disconnect() }
            if (downloaded != expectedBytes || digest.digest().joinToString("") {
                    "%02x".format(it.toInt() and 0xff)
                } != expectedSha) throw IOException("APK checksum mismatch")
            if (!partial.renameTo(target)) throw IOException("Failed to finalize verified APK")
            onProgress(100)
            target
        } finally { partial.delete() }
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                md.update(buffer, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    suspend fun verifyInstalledPackageCandidate(release: TempoRelease): File = withContext(Dispatchers.IO) {
        val file = downloadedFile(release.tag) ?: throw IOException("Invalid release name")
        if (!file.isFile || file.length() != release.apkBytes || sha256(file) != release.sha256)
            throw IOException("Downloaded update failed verification. Download it again.")
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val flag = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        @Suppress("DEPRECATION")
        val archive = pm.getPackageArchiveInfo(file.absolutePath, flag)
            ?: throw IOException("Downloaded file is not an APK")
        @Suppress("DEPRECATION")
        val installed = pm.getPackageInfo(context.packageName, flag)
        if (archive.packageName != context.packageName) throw IOException("Package identity mismatch")
        val archiveCode = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else archive.versionCode.toLong()
        val installedCode = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
        if (archiveCode <= installedCode) throw IOException("This APK cannot update the installed version (version code is not newer)")
        @Suppress("DEPRECATION")
        fun signerHashes(info: android.content.pm.PackageInfo): Set<String> {
            val signatures = if (Build.VERSION.SDK_INT >= 28)
                info.signingInfo?.apkContentsSigners else info.signatures
            return signatures.orEmpty().map { signature ->
                MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                    .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            }.toSet()
        }
        if (signerHashes(archive).isEmpty() || signerHashes(archive) != signerHashes(installed))
            throw IOException("APK signer differs from the installed Tempo. An in-place update is not possible.")
        file
    }

    fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    fun requestInstallPermission() {
        if (Build.VERSION.SDK_INT >= 26) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun openSystemInstaller(file: File) {
        if (!canInstallPackages()) throw IOException("Allow Tempo to install updates in Android settings first")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
