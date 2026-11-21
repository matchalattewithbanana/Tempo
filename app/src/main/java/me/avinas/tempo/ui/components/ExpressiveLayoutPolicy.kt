package me.avinas.tempo.ui.components

/**
 * Pure layout thresholds shared by connected controls.
 *
 * Compact phones and accessibility font scales take precedence over a wide
 * physical window. The four-choice case becomes 2×2 on standard phones,
 * avoiding an oversized fourth button alone on a trailing row.
 */
internal fun connectedChoiceColumns(
    widthDp: Int,
    fontScale: Float,
    optionCount: Int,
): Int {
    require(optionCount >= 0)
    if (optionCount == 0) return 1
    val max = when {
        widthDp < 360 || fontScale >= 1.3f -> 2
        widthDp >= 600 && fontScale < 1.2f -> 5
        else -> 3
    }
    return (if (optionCount == 4 && max == 3) 2 else max).coerceAtMost(optionCount)
}

/**
 * Material 3 Expressive recommends connected groups for small sets of related
 * single choices and filter chips instead of large segmented control sets.
 */
internal fun shouldUseFilterChips(optionCount: Int): Boolean {
    require(optionCount >= 0)
    return optionCount > 5
}
