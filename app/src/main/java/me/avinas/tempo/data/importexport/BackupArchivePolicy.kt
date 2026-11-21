package me.avinas.tempo.data.importexport

/** Canonical limits and path rules for Tempo backup ZIPs. */
internal object BackupArchivePolicy {
    const val IMAGES_DIR = "images/"
    const val MAX_BUNDLED_IMAGE_BYTES = 10L * 1024L * 1024L
    const val MAX_TOTAL_IMAGE_BYTES = 50L * 1024L * 1024L
    const val MAX_DATA_JSON_BYTES = 1024L * 1024L * 1024L
    const val MAX_IMPORTED_EVENTS = 5_000_000
    const val MAX_IMPORTED_ARCHIVE_ROWS = 2_000_000

    fun sanitizeBundledImageName(bundledName: String): String? {
        val candidate = bundledName.trim()
        if (candidate.isBlank() || candidate.length > 128) return null
        if (
            candidate.contains('/') ||
            candidate.contains('\\') ||
            candidate == "." ||
            candidate == ".." ||
            candidate.contains("..")
        ) {
            return null
        }
        return candidate
    }
}
