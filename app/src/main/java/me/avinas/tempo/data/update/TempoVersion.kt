package me.avinas.tempo.data.update

/**
 * Version comparison for maintained Tempo release tags.
 * Final releases sort above betas of the same version; unknown legacy fork
 * tags are excluded from automatic installation rather than guessed.
 */
data class TempoVersion(
    val major: Int, val minor: Int, val patch: Int, val beta: Int? = null,
) : Comparable<TempoVersion> {
    override fun compareTo(other: TempoVersion): Int {
        for ((a, b) in listOf(major to other.major, minor to other.minor, patch to other.patch)) {
            if (a != b) return a.compareTo(b)
        }
        if (beta == null && other.beta != null) return 1
        if (beta != null && other.beta == null) return -1
        return (beta ?: 0).compareTo(other.beta ?: 0)
    }

    companion object {
        private val PATTERN = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:-beta\.(\d+))?$""")
        fun parse(value: String): TempoVersion? {
            val match = PATTERN.matchEntire(value.trim()) ?: return null
            return runCatching {
                TempoVersion(match.groupValues[1].toInt(), match.groupValues[2].toInt(),
                    match.groupValues[3].toInt(), match.groupValues[4].takeIf { it.isNotEmpty() }?.toInt())
            }.getOrNull()
        }
    }
}

enum class UpdateChannel { STABLE, PREVIEW }

data class TempoRelease(
    val tag: String,
    val version: TempoVersion,
    val name: String,
    val notes: String,
    val publishedAt: String,
    val prerelease: Boolean,
    val pageUrl: String,
    val apkUrl: String,
    val apkName: String,
    val apkBytes: Long,
    val sha256: String,
)
