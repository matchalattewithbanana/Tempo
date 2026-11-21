package me.avinas.tempo.ui.appearance

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color

/**
 * Shared design tokens. These intentionally keep the legacy Abyss values unchanged.
 * Presets have dedicated Light surface colors as well as Dark surface roles.
 * Some artwork-specific illustrations intentionally retain their design colors.
 */
data class AppearancePalette(
    val preset: AppearancePreset,
    val accent: Color,
    val onAccent: Color,
    val background: Color,
    val topBackground: Color,
    val surface: Color,
    val elevatedSurface: Color,
    val surfaceSunken: Color,
    val onSurface: Color,
    val mutedText: Color,
    val border: Color,
    val isDark: Boolean = true,
    val secondaryAccent: Color? = null,
    val tertiaryAccent: Color? = null,
) {
    fun materialColorScheme(): ColorScheme = (if (isDark) darkColorScheme() else lightColorScheme()).copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = elevatedSurface,
        onPrimaryContainer = onSurface,
        secondary = secondaryAccent ?: accent,
        onSecondary = secondaryAccent?.let { if (it.luminance() >= 0.179f) Color.Black else Color.White } ?: onAccent,
        secondaryContainer = secondaryAccent?.let { lerp(elevatedSurface, it, 0.10f) } ?: elevatedSurface,
        onSecondaryContainer = onSurface,
        tertiary = tertiaryAccent ?: accent,
        onTertiary = tertiaryAccent?.let { if (it.luminance() >= 0.179f) Color.Black else Color.White } ?: onAccent,
        // Keep the tertiary container/text pair together for every custom seed.
        tertiaryContainer = tertiaryAccent?.let { lerp(elevatedSurface, it, 0.10f) } ?: elevatedSurface,
        onTertiaryContainer = onSurface,
        background = background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = elevatedSurface,
        onSurfaceVariant = mutedText,
        surfaceContainerLowest = surfaceSunken,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = elevatedSurface,
        surfaceContainerHighest = elevatedSurface,
        surfaceDim = surfaceSunken,
        surfaceBright = elevatedSurface,
        inverseSurface = onSurface,
        inverseOnSurface = background,
        // Snackbar action text sits on inverseSurface, not on the app surface.
        // onAccent is the contrasting polarity for the inverse surface.
        inversePrimary = onAccent,
        surfaceTint = accent,
        outline = border,
        outlineVariant = border.copy(alpha = 0.55f),
    )
}

object AppearancePaletteResolver {
    fun forPreset(preset: AppearancePreset): AppearancePalette = when (preset) {
        AppearancePreset.MIDNIGHT_BLOOM -> AppearancePalette(
            preset = preset,
            accent = Color(0xFFD7A2E9), onAccent = Color(0xFF21192C),
            background = Color(0xFF14101E), topBackground = Color(0xFF281C36),
            surface = Color(0xFF211A2B), elevatedSurface = Color(0xFF352741),
            surfaceSunken = Color(0xFF0E0B17), onSurface = Color(0xFFF5EDFA),
            mutedText = Color(0xFFBBAACD), border = Color(0xFF5D4B6A),
        )
        AppearancePreset.SAKURA -> AppearancePalette(
            preset = preset,
            accent = Color(0xFFFFB2C3), onAccent = Color(0xFF341724),
            background = Color(0xFF241316), topBackground = Color(0xFF3B2228),
            surface = Color(0xFF322024), elevatedSurface = Color(0xFF4A2C33),
            surfaceSunken = Color(0xFF190D11), onSurface = Color(0xFFFFF0F2),
            mutedText = Color(0xFFD4B0BA), border = Color(0xFF76505A),
        )
        AppearancePreset.MATCHA -> AppearancePalette(
            preset = preset,
            accent = Color(0xFFBEDB89), onAccent = Color(0xFF263417),
            background = Color(0xFF151C14), topBackground = Color(0xFF283626),
            surface = Color(0xFF222C20), elevatedSurface = Color(0xFF354532),
            surfaceSunken = Color(0xFF0D120C), onSurface = Color(0xFFF1F7E8),
            mutedText = Color(0xFFB9CBAD), border = Color(0xFF536A4D),
        )
        AppearancePreset.COBALT -> AppearancePalette(
            preset = preset,
            accent = Color(0xFF8CAAFF), onAccent = Color(0xFF15284C),
            background = Color(0xFF0A1431), topBackground = Color(0xFF18284F),
            surface = Color(0xFF132446), elevatedSurface = Color(0xFF243B65),
            surfaceSunken = Color(0xFF071024), onSurface = Color(0xFFEEF3FF),
            mutedText = Color(0xFFAEC0E8), border = Color(0xFF4B6495),
        )
        AppearancePreset.PORCELAIN -> AppearancePalette(
            preset = preset,
            accent = Color(0xFF9CDBEE), onAccent = Color(0xFF1E3442),
            background = Color(0xFF101923), topBackground = Color(0xFF21323C),
            surface = Color(0xFF192831), elevatedSurface = Color(0xFF2A414D),
            surfaceSunken = Color(0xFF0B1219), onSurface = Color(0xFFE9F6FB),
            mutedText = Color(0xFFADCAD5), border = Color(0xFF4B6976),
        )
        AppearancePreset.TERRACOTTA -> AppearancePalette(
            preset = preset,
            accent = Color(0xFFF1AB8B), onAccent = Color(0xFF3A2018),
            background = Color(0xFF241715), topBackground = Color(0xFF3B2922),
            surface = Color(0xFF35231E), elevatedSurface = Color(0xFF4D3027),
            surfaceSunken = Color(0xFF190F0D), onSurface = Color(0xFFFFF0E8),
            mutedText = Color(0xFFD7B5A5), border = Color(0xFF795342),
        )
        AppearancePreset.CITRINE -> AppearancePalette(
            preset = preset,
            accent = Color(0xFFD4EE73), onAccent = Color(0xFF29321B),
            background = Color(0xFF151A12), topBackground = Color(0xFF293223),
            surface = Color(0xFF232B1F), elevatedSurface = Color(0xFF37402B),
            surfaceSunken = Color(0xFF0C100A), onSurface = Color(0xFFF5F8D7),
            mutedText = Color(0xFFC6D0AC), border = Color(0xFF63734A),
        )
        AppearancePreset.MOCHA -> AppearancePalette(
            preset = preset,
            accent = Color(0xFFE4BAA0), onAccent = Color(0xFF352319),
            background = Color(0xFF1B1513), topBackground = Color(0xFF33241F),
            surface = Color(0xFF2A201B), elevatedSurface = Color(0xFF423127),
            surfaceSunken = Color(0xFF120D0B), onSurface = Color(0xFFFAEFE5),
            mutedText = Color(0xFFCDB5A5), border = Color(0xFF6F5343),
        )
        AppearancePreset.LAGOON -> AppearancePalette(
            preset = preset,
            accent = Color(0xFF70E1CE), onAccent = Color(0xFF14312E),
            background = Color(0xFF0A1E20), topBackground = Color(0xFF164045),
            surface = Color(0xFF123236), elevatedSurface = Color(0xFF1D4B4D),
            surfaceSunken = Color(0xFF071416), onSurface = Color(0xFFE6FFF7),
            mutedText = Color(0xFFABD5CA), border = Color(0xFF397778),
        )
        AppearancePreset.LILAC -> AppearancePalette(
            preset = preset,
            accent = Color(0xFFD9B6F9), onAccent = Color(0xFF362344),
            background = Color(0xFF21172A), topBackground = Color(0xFF392347),
            surface = Color(0xFF30203C), elevatedSurface = Color(0xFF4B3158),
            surfaceSunken = Color(0xFF160E1D), onSurface = Color(0xFFF5EAFE),
            mutedText = Color(0xFFC8B0DA), border = Color(0xFF725681),
        )

        AppearancePreset.EMBER -> AppearancePalette(
            preset = preset, accent = Color(0xFFFF91A3), onAccent = Color(0xFF300D18),
            background = Color(0xFF1C0D15), topBackground = Color(0xFF301822),
            surface = Color(0xFF28151E), elevatedSurface = Color(0xFF3A202B),
            surfaceSunken = Color(0xFF150910), onSurface = Color(0xFFFFEDF1),
            mutedText = Color(0xFFD0AEB8), border = Color(0xFF70414D),
        )
        AppearancePreset.SOLSTICE -> AppearancePalette(
            preset = preset, accent = Color(0xFFF0B47D), onAccent = Color(0xFF291508),
            background = Color(0xFF1B1510), topBackground = Color(0xFF30251C),
            surface = Color(0xFF2A2119), elevatedSurface = Color(0xFF3B2E22),
            surfaceSunken = Color(0xFF14100C), onSurface = Color(0xFFFFF3E4),
            mutedText = Color(0xFFCAB9A4), border = Color(0xFF705A42),
        )
        AppearancePreset.ABYSS -> AppearancePalette(
            preset = preset,
            accent = Color(0xFF2FDBB8), onAccent = Color(0xFF0A0E0E),
            background = Color(0xFF0A0E0E), topBackground = Color(0xFF101414),
            surface = Color(0xFF101414), elevatedSurface = Color(0xFF141818),
            surfaceSunken = Color(0xFF080A0A),
            onSurface = Color(0xFFE6EAEA), mutedText = Color(0xFF9AA2A2),
            border = Color(0xFF33403E),
        )
        AppearancePreset.OBSIDIAN_GOLD -> AppearancePalette(
            preset = preset,
            accent = Color(0xFFDDBA70), onAccent = Color(0xFF17110A),
            background = Color(0xFF050608), topBackground = Color(0xFF17191B),
            surface = Color(0xFF17191B), elevatedSurface = Color(0xFF242528),
            surfaceSunken = Color(0xFF050506),
            onSurface = Color(0xFFF0E7D2), mutedText = Color(0xFFAEA595),
            border = Color(0xFF65553B),
        )
        AppearancePreset.AURORA -> AppearancePalette(
            preset = preset,
            accent = Color(0xFFA78BFA), onAccent = Color(0xFF120B2B),
            background = Color(0xFF0F102A), topBackground = Color(0xFF211C40),
            surface = Color(0xFF20203C), elevatedSurface = Color(0xFF302A54),
            surfaceSunken = Color(0xFF0B0A20),
            onSurface = Color(0xFFF2ECFF), mutedText = Color(0xFFB7AFD2),
            border = Color(0xFF544776),
        )
        AppearancePreset.VERDANT -> AppearancePalette(
            preset = preset, accent = Color(0xFFACD47E), onAccent = Color(0xFF16230D),
            background = Color(0xFF111C12), topBackground = Color(0xFF233524),
            surface = Color(0xFF1B2C1D), elevatedSurface = Color(0xFF293D29),
            surfaceSunken = Color(0xFF0B140D), onSurface = Color(0xFFF1F7E8),
            mutedText = Color(0xFFB9C9AF), border = Color(0xFF526E50),
        )
        AppearancePreset.SLATE -> AppearancePalette(
            preset = preset, accent = Color(0xFFBDC9DB), onAccent = Color(0xFF18202D),
            background = Color(0xFF16191F), topBackground = Color(0xFF272D38),
            surface = Color(0xFF1E252E), elevatedSurface = Color(0xFF303946),
            surfaceSunken = Color(0xFF101218), onSurface = Color(0xFFF1F3F7),
            mutedText = Color(0xFFBAC2CF), border = Color(0xFF576273),
        )
        // Glacier Night can also resolve to the experimental Glacier Light palette.
        AppearancePreset.GLACIER -> AppearancePalette(
            preset = preset,
            accent = Color(0xFF72D0F2), onAccent = Color(0xFF082638),
            background = Color(0xFF0A1A27), topBackground = Color(0xFF173449),
            surface = Color(0xFF142A3C), elevatedSurface = Color(0xFF213F53),
            surfaceSunken = Color(0xFF07151F),
            onSurface = Color(0xFFF1F9FF), mutedText = Color(0xFFA9C5D7),
            border = Color(0xFF436478),
        )
    }

    fun resolve(settings: AppearanceSettings, systemDark: Boolean = true): AppearancePalette {
        val dark = when (settings.tone) {
            AppearanceTone.LIGHT -> false
            AppearanceTone.SYSTEM -> systemDark
            else -> true
        }
        val base = forPreset(settings.preset)
        val palette = when {
            settings.tone == AppearanceTone.OLED -> base.copy(
                background = Color.Black, topBackground = Color.Black, surfaceSunken = Color.Black,
            )
            dark -> base
            else -> lightPalette(base)
        }
        val custom = if (settings.accentSource == AppearanceAccentSource.CUSTOM) {
            normalizeAccentHex(settings.customAccent)?.toLong(16)
        } else null
        val accented = if (custom != null) withAccent(palette, Color(0xFF000000L or custom)) else palette
        if (settings.accentSource != AppearanceAccentSource.CUSTOM) return accented
        // Optional seed colors can appear on cards, raised surfaces and inset surfaces.
        // Adjust against every listening surface rather than the base surface alone.
        fun seed(raw: String?): Color? = normalizeAccentHex(raw)
            ?.toLong(16)?.let { Color(0xFF000000L or it) }
            ?.let { candidate ->
                contrastAdjusted(
                    candidate,
                    if (accented.isDark) Color.White else Color.Black,
                    listOf(accented.surface, accented.elevatedSurface, accented.surfaceSunken),
                )
            }
        return accented.copy(
            secondaryAccent = seed(settings.customSecondary),
            tertiaryAccent = seed(settings.customTertiary),
        )
    }

    private fun lightPalette(base: AppearancePalette): AppearancePalette {
        // The new editions use fully authored light surfaces, not generic dark tints.
        // Each light palette has its own canvas, raised card, inset, text and border.
        when (base.preset) {
            AppearancePreset.MIDNIGHT_BLOOM -> return base.copy(
                accent = Color(0xFF72438B), onAccent = Color(0xFFFFFFFF),
                background = Color(0xFFF6F0FA), topBackground = Color(0xFFF0E6F6),
                surface = Color(0xFFFFFCFE), elevatedSurface = Color(0xFFE9DCF0),
                surfaceSunken = Color(0xFFE3D3EA), onSurface = Color(0xFF24182C),
                mutedText = Color(0xFF5C4E63), border = Color(0xFFA98CB6),
                isDark = false,
            )
            AppearancePreset.SAKURA -> return base.copy(
                accent = Color(0xFF9D3553), onAccent = Color(0xFFFFFFFF),
                background = Color(0xFFFFF1F3), topBackground = Color(0xFFFFE3E9),
                surface = Color(0xFFFFF9FA), elevatedSurface = Color(0xFFF6DDE3),
                surfaceSunken = Color(0xFFF2CED9), onSurface = Color(0xFF351923),
                mutedText = Color(0xFF724C57), border = Color(0xFFD3A5AF),
                isDark = false,
            )
            AppearancePreset.MATCHA -> return base.copy(
                accent = Color(0xFF3A6034), onAccent = Color(0xFFFFFFFF),
                background = Color(0xFFF2F5EA), topBackground = Color(0xFFE9F0DF),
                surface = Color(0xFFFEFFF7), elevatedSurface = Color(0xFFE0EAD4),
                surfaceSunken = Color(0xFFD6E3C8), onSurface = Color(0xFF253022),
                mutedText = Color(0xFF52654C), border = Color(0xFFA4BA93),
                isDark = false,
            )
            AppearancePreset.COBALT -> return base.copy(
                accent = Color(0xFF264DAD), onAccent = Color(0xFFFFFFFF),
                background = Color(0xFFEDF2FF), topBackground = Color(0xFFE1EAFF),
                surface = Color(0xFFFAFCFF), elevatedSurface = Color(0xFFDCE7FB),
                surfaceSunken = Color(0xFFD0DFFA), onSurface = Color(0xFF151F40),
                mutedText = Color(0xFF465678), border = Color(0xFF9EB5DD),
                isDark = false,
            )
            AppearancePreset.PORCELAIN -> return base.copy(
                accent = Color(0xFF16617E), onAccent = Color(0xFFFFFFFF),
                background = Color(0xFFF7F8F5), topBackground = Color(0xFFEEF5F4),
                surface = Color(0xFFFFFFFF), elevatedSurface = Color(0xFFE5F0F1),
                surfaceSunken = Color(0xFFDBE9E9), onSurface = Color(0xFF1C2B31),
                mutedText = Color(0xFF50636B), border = Color(0xFFA6BDC1),
                isDark = false,
            )
            AppearancePreset.TERRACOTTA -> return base.copy(
                accent = Color(0xFF8D3E27), onAccent = Color(0xFFFFFFFF),
                background = Color(0xFFFFF2EA), topBackground = Color(0xFFFCE7DA),
                surface = Color(0xFFFFFBF6), elevatedSurface = Color(0xFFF5E0D1),
                surfaceSunken = Color(0xFFF1D3BE), onSurface = Color(0xFF35211B),
                mutedText = Color(0xFF70534A), border = Color(0xFFC9A28F),
                isDark = false,
            )
            AppearancePreset.CITRINE -> return base.copy(
                accent = Color(0xFF506514), onAccent = Color(0xFFFFFFFF),
                background = Color(0xFFF7F8E8), topBackground = Color(0xFFF0F5D8),
                surface = Color(0xFFFFFFF8), elevatedSurface = Color(0xFFEBF0CF),
                surfaceSunken = Color(0xFFE0E8B9), onSurface = Color(0xFF283014),
                mutedText = Color(0xFF596244), border = Color(0xFFB7C78E),
                isDark = false,
            )
            AppearancePreset.MOCHA -> return base.copy(
                accent = Color(0xFF76513C), onAccent = Color(0xFFFFFFFF),
                background = Color(0xFFF7F0EA), topBackground = Color(0xFFF1E5D9),
                surface = Color(0xFFFEFBF7), elevatedSurface = Color(0xFFEDE0D5),
                surfaceSunken = Color(0xFFE3D2C2), onSurface = Color(0xFF30231D),
                mutedText = Color(0xFF6A564B), border = Color(0xFFC4AC99),
                isDark = false,
            )
            AppearancePreset.LAGOON -> return base.copy(
                accent = Color(0xFF006A69), onAccent = Color(0xFFFFFFFF),
                background = Color(0xFFEAF8F5), topBackground = Color(0xFFDAF3ED),
                surface = Color(0xFFF7FFFD), elevatedSurface = Color(0xFFD1F0E9),
                surfaceSunken = Color(0xFFC3E7DF), onSurface = Color(0xFF123331),
                mutedText = Color(0xFF3B625F), border = Color(0xFF9BCBC4),
                isDark = false,
            )
            AppearancePreset.LILAC -> return base.copy(
                accent = Color(0xFF75449C), onAccent = Color(0xFFFFFFFF),
                background = Color(0xFFF7F0FE), topBackground = Color(0xFFEFE3FB),
                surface = Color(0xFFFFFBFF), elevatedSurface = Color(0xFFEBDCFA),
                surfaceSunken = Color(0xFFE2D1F1), onSurface = Color(0xFF30213C),
                mutedText = Color(0xFF65546F), border = Color(0xFFC2A5D6),
                isDark = false,
            )
            else -> Unit
        }
        val (canvas, accent) = when (base.preset) {
            AppearancePreset.ABYSS -> Color(0xFFEAF5F0) to Color(0xFF00614E)
            AppearancePreset.OBSIDIAN_GOLD -> Color(0xFFF6EED8) to Color(0xFF6A4B05)
            AppearancePreset.AURORA -> Color(0xFFF0EAF9) to Color(0xFF5E3799)
            AppearancePreset.GLACIER -> Color(0xFFE8F2FA) to Color(0xFF155782)
            AppearancePreset.EMBER -> Color(0xFFFAE9EF) to Color(0xFF942443)
            AppearancePreset.SOLSTICE -> Color(0xFFFFEEDC) to Color(0xFF8B3A1B)
            AppearancePreset.VERDANT -> Color(0xFFEDF4E3) to Color(0xFF3D5E22)
            AppearancePreset.SLATE -> Color(0xFFEDF0F4) to Color(0xFF46556D)
            else -> error("New light editions handled above")
        }
        return base.copy(
            accent = accent,
            onAccent = Color.White,
            background = canvas,
            topBackground = lerp(canvas, Color.White, 0.68f),
            surface = lerp(canvas, Color.White, 0.55f),
            elevatedSurface = lerp(canvas, accent, 0.08f),
            surfaceSunken = lerp(canvas, accent, 0.14f),
            onSurface = lerp(Color(0xFF17191D), accent, 0.20f),
            mutedText = lerp(Color(0xFF404650), accent, 0.26f),
            border = lerp(accent, canvas, 0.38f),
            isDark = false,
        )
    }

    /** Resolve candidate text or icon color against the surfaces behind it. */
    fun readableAccent(palette: AppearancePalette, candidate: Color, surface: Color = palette.surface): Color =
        contrastAdjusted(candidate, if (palette.isDark) Color.White else Color.Black, listOf(surface))

    /** Adjust decorative accent until it is readable against its actual surface. */
    fun withAccent(palette: AppearancePalette, candidate: Color): AppearancePalette {
        val target = if (palette.isDark) Color.White else Color.Black
        val accent = contrastAdjusted(
            candidate, target, listOf(palette.surface, palette.elevatedSurface, palette.surfaceSunken),
        )
        val onAccent = if (contrast(Color.Black, accent) >= contrast(Color.White, accent)) Color.Black else Color.White
        return palette.copy(accent = accent, onAccent = onAccent)
    }

    private fun contrastAdjusted(candidate: Color, target: Color, surfaces: List<Color>): Color {
        val opaque = candidate.copy(alpha = 1f)
        for (step in 0..20) {
            val color = lerp(opaque, target, step / 20f)
            if (surfaces.all { contrast(color, it) >= 4.5f }) return color
        }
        return target
    }

    private fun contrast(a: Color, b: Color): Float {
        val x = a.luminance(); val y = b.luminance()
        return (maxOf(x, y) + 0.05f) / (minOf(x, y) + 0.05f)
    }

}
