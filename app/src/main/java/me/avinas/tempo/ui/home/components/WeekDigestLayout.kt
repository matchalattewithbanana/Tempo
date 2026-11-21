package me.avinas.tempo.ui.home.components

/** Pure layout rule for the weekly digest; 1.3×+ text must not be cramped. */
internal fun weekDigestColumns(widthDp: Float, fontScale: Float): Int =
    if (widthDp < 360f || fontScale >= 1.3f) 1 else 2
