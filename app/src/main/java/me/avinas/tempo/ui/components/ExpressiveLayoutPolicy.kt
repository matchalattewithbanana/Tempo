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


/** Canonical Material 3 width policy shared across Tempo layouts. */
internal enum class TempoWidthClass { COMPACT, MEDIUM, EXPANDED, LARGE, EXTRA_LARGE }

internal enum class TempoPrimaryNavigation { BOTTOM_BAR, COLLAPSED_RAIL, EXPANDED_RAIL }

internal data class TempoAdaptiveLayout(
    val widthClass: TempoWidthClass,
    val primaryNavigation: TempoPrimaryNavigation,
    val contentMarginDp: Float,
    val supportsTwoPane: Boolean,
)

internal fun tempoAdaptiveLayout(widthDp: Float): TempoAdaptiveLayout {
    require(widthDp >= 0f)
    val widthClass = when {
        widthDp < 600f -> TempoWidthClass.COMPACT
        widthDp < 840f -> TempoWidthClass.MEDIUM
        widthDp < 1200f -> TempoWidthClass.EXPANDED
        widthDp < 1600f -> TempoWidthClass.LARGE
        else -> TempoWidthClass.EXTRA_LARGE
    }
    return TempoAdaptiveLayout(
        widthClass = widthClass,
        primaryNavigation = when (widthClass) {
            TempoWidthClass.COMPACT -> TempoPrimaryNavigation.BOTTOM_BAR
            TempoWidthClass.MEDIUM -> TempoPrimaryNavigation.COLLAPSED_RAIL
            TempoWidthClass.EXPANDED,
            TempoWidthClass.LARGE,
            TempoWidthClass.EXTRA_LARGE -> TempoPrimaryNavigation.EXPANDED_RAIL
        },
        contentMarginDp = if (widthClass == TempoWidthClass.COMPACT) 16f else 24f,
        supportsTwoPane = widthClass >= TempoWidthClass.EXPANDED,
    )
}
