package me.avinas.tempo.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

/** Locale observed through Compose configuration so language changes trigger recomposition. */
@Composable
internal fun observableLocale(): Locale = LocalConfiguration.current.locales[0]

/** Locale-aware first-character casing without a non-observable Locale.getDefault() read. */
@Composable
internal fun String.capitalizeForObservableLocale(): String {
    val locale = observableLocale()
    return replaceFirstChar { character -> character.uppercase(locale) }
}
