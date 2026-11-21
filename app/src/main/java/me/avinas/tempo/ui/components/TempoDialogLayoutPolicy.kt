package me.avinas.tempo.ui.components

/**
 * Pure, predictable layout policy for primary/secondary dialog actions.
 * The thresholds are Tempo design choices, not published M3 dimensions.
 */
internal fun stackTempoDialogActions(widthDp: Float, fontScale: Float): Boolean =
    widthDp < 360f || fontScale >= 1.3f
