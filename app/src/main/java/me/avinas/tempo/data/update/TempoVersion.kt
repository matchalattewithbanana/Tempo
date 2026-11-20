package me.avinas.tempo.data.update

/**
 * Version comparison for maintained Tempo release tags.
 * Final releases sort above betas of the same version. New betas have no
 * numeric suffix; historical -beta.N and -fork tags remain readable for
 * comparisons without renaming published releases.
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
        // New tags use -beta only; retain parsing of historical -beta.N.
        private val PATTERN = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:-fork\.\d+)?(?:-beta(?:\.(\d+))?|-preview)?$""")
        fun parse(value: String): TempoVersion? {
            val match = PATTERN.matchEntire(value.trim()) ?: return null
            return runCatching {
                val prerelease = match.value.contains("-fork.") ||
                    match.value.endsWith("-preview") || match.value.endsWith("-beta")
                TempoVersion(
                    match.groupValues[1].toInt(), match.groupValues[2].toInt(),
                    match.groupValues[3].toInt(),
                    match.groupValues[4].takeIf { it.isNotEmpty() }?.toInt()
                        ?: if (prerelease) 0 else null,
                )
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
