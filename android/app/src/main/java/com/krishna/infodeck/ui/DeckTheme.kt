package com.krishna.infodeck.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.krishna.infodeck.R
import com.krishna.infodeck.core.DeckStyle
import com.krishna.infodeck.core.Hue

/**
 * 2.0 design system (design 1.15.0.D1). Every colour the screens draw comes from the
 * active [DeckPalette] through [LocalDeck]; a hue keeps its meaning in every theme
 * (amber = time, rose = trouble, green = share/OK, purple = file, violet = tag/QR,
 * teal = settings, sky = data) — only the tones change.
 */
data class HueSet(val glyph: Color, val glyph2: Color, val tint: Color, val onTint: Color)

enum class NavStyle { PILL, GLASS, CENTER }

data class DeckPalette(
    val style: DeckStyle,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val ink2: Color,
    val hint: Color,
    val line: Color,
    val outline: Color,
    /** Hero, FAB and Save gradient stops. */
    val brand: List<Color>,
    val onBrand: Color,
    val hues: Map<Hue, HueSet>,
    val body: FontFamily,
    val heading: FontFamily,
    val nav: NavStyle,
    /** Sunrise draws each Home card as a gradient wallet card. */
    val walletRows: Boolean,
    /** Neon draws a soft glow around tiles and the active tab. */
    val glow: Boolean,
    /** The Home hero sits on a coloured band (Aurora, Neon) or on the page (Sunrise). */
    val heroOnColour: Boolean
) {
    val dark: Boolean get() = style.dark
    fun hue(h: Hue): HueSet = hues[h] ?: hues.getValue(Hue.SLATE)
}

private fun hs(g: Long, g2: Long, t: Long, on: Long) = HueSet(Color(g), Color(g2), Color(t), Color(on))
private fun neon(g: Long, g2: Long) = HueSet(Color(g), Color(g2), Color(g).copy(alpha = 0.16f), Color(g))

val PoppinsFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_extrabold, FontWeight.ExtraBold)
)
val DmSansFamily = FontFamily(
    Font(R.font.dmsans_regular, FontWeight.Normal),
    Font(R.font.dmsans_medium, FontWeight.Medium),
    Font(R.font.dmsans_medium, FontWeight.SemiBold),
    Font(R.font.dmsans_bold, FontWeight.Bold),
    Font(R.font.dmsans_extrabold, FontWeight.ExtraBold)
)
val NunitoFamily = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold)
)
val FredokaFamily = FontFamily(
    Font(R.font.fredoka_medium, FontWeight.Normal),
    Font(R.font.fredoka_medium, FontWeight.Medium),
    Font(R.font.fredoka_semibold, FontWeight.SemiBold),
    Font(R.font.fredoka_bold, FontWeight.Bold),
    Font(R.font.fredoka_bold, FontWeight.ExtraBold)
)

/** Option 1 — the default light theme. */
val AuroraPalette = DeckPalette(
    style = DeckStyle.AURORA,
    bg = Color(0xFFF5F3FF), surface = Color.White, surface2 = Color(0xFFF1EFFB),
    ink = Color(0xFF1E1B4B), ink2 = Color(0xFF625F84), hint = Color(0xFF9F9CBB),
    line = Color(0xFFE8E5F7), outline = Color(0xFFC9C5E6),
    brand = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFFDB2777)),
    onBrand = Color.White,
    hues = mapOf(
        Hue.INDIGO to hs(0xFF4F46E5, 0xFF8B5CF6, 0xFFEEF0FF, 0xFF3730A3),
        Hue.AMBER to hs(0xFFF59E0B, 0xFFF97316, 0xFFFFF4DE, 0xFFB45309),
        Hue.ROSE to hs(0xFFF43F5E, 0xFFE11D48, 0xFFFFE4EA, 0xFFBE123C),
        Hue.GREEN to hs(0xFF10B981, 0xFF059669, 0xFFDDF8EE, 0xFF047857),
        Hue.VIOLET to hs(0xFF8B5CF6, 0xFFD946EF, 0xFFF1EAFE, 0xFF6D28D9),
        Hue.PURPLE to hs(0xFFA855F7, 0xFF7E22CE, 0xFFF3E8FF, 0xFF7E22CE),
        Hue.TEAL to hs(0xFF14B8A6, 0xFF0EA5E9, 0xFFDDF7F3, 0xFF0F766E),
        Hue.SKY to hs(0xFF3B82F6, 0xFF06B6D4, 0xFFE0EDFF, 0xFF1D4ED8),
        Hue.SLATE to hs(0xFF64748B, 0xFF475569, 0xFFEEF1F5, 0xFF475569)
    ),
    body = PoppinsFamily, heading = PoppinsFamily,
    nav = NavStyle.PILL, walletRows = false, glow = false, heroOnColour = true
)

/** Option 3 — the dark theme. */
val NeonPalette = DeckPalette(
    style = DeckStyle.NEON,
    bg = Color(0xFF0A0E1F), surface = Color(0xFF141A31), surface2 = Color(0xFF1C2340),
    ink = Color(0xFFEEF2FF), ink2 = Color(0xFF98A2C9), hint = Color(0xFF6A749C),
    line = Color(0xFF252D50), outline = Color(0xFF3B4472),
    brand = listOf(Color(0xFF22D3EE), Color(0xFFA78BFA)),
    onBrand = Color(0xFF0A0E1F),
    hues = mapOf(
        Hue.INDIGO to neon(0xFF22D3EE, 0xFFA78BFA),
        Hue.AMBER to neon(0xFFFBBF24, 0xFFFB923C),
        Hue.ROSE to neon(0xFFFB7185, 0xFFF43F5E),
        Hue.GREEN to neon(0xFFA3E635, 0xFF4ADE80),
        Hue.VIOLET to neon(0xFFF472B6, 0xFFE879F9),
        Hue.PURPLE to neon(0xFFC084FC, 0xFFA855F7),
        Hue.TEAL to neon(0xFF2DD4BF, 0xFF22D3EE),
        Hue.SKY to neon(0xFF38BDF8, 0xFF60A5FA),
        Hue.SLATE to neon(0xFF94A3B8, 0xFF64748B)
    ),
    body = DmSansFamily, heading = DmSansFamily,
    nav = NavStyle.GLASS, walletRows = false, glow = true, heroOnColour = true
)

/** Option 4 — the custom theme. */
val SunrisePalette = DeckPalette(
    style = DeckStyle.SUNRISE,
    bg = Color(0xFFFFF7EE), surface = Color.White, surface2 = Color(0xFFFBEFE3),
    ink = Color(0xFF2B1B17), ink2 = Color(0xFF7A6259), hint = Color(0xFFB09A90),
    line = Color(0xFFF1E2D6), outline = Color(0xFFDCC5B4),
    brand = listOf(Color(0xFFFF7A59), Color(0xFFFF3D8B)),
    onBrand = Color.White,
    hues = mapOf(
        Hue.INDIGO to hs(0xFFFF5A6E, 0xFFFF3D8B, 0xFFFFE6E2, 0xFFC42345),
        Hue.AMBER to hs(0xFFF7971E, 0xFFFFB300, 0xFFFFF1D6, 0xFFB45309),
        Hue.ROSE to hs(0xFFE5383B, 0xFFFF5E62, 0xFFFFE1E1, 0xFFB91C1C),
        Hue.GREEN to hs(0xFF11998E, 0xFF38B27D, 0xFFDCF5EC, 0xFF0B7268),
        Hue.VIOLET to hs(0xFFB24592, 0xFFF15F79, 0xFFFDE3EC, 0xFF9D174D),
        Hue.PURPLE to hs(0xFF8E2DE2, 0xFFB266F0, 0xFFF1E4FD, 0xFF6B21A8),
        Hue.TEAL to hs(0xFF2193B0, 0xFF43C6DB, 0xFFDDF3F8, 0xFF155E75),
        Hue.SKY to hs(0xFF3A7BD5, 0xFF00B4DB, 0xFFE0EDFB, 0xFF1E4FA0),
        Hue.SLATE to hs(0xFF8D7B73, 0xFF6F5E57, 0xFFF2EAE4, 0xFF5B4A43)
    ),
    body = NunitoFamily, heading = FredokaFamily,
    nav = NavStyle.CENTER, walletRows = true, glow = false, heroOnColour = false
)

fun paletteFor(style: DeckStyle): DeckPalette = when (style) {
    DeckStyle.AURORA -> AuroraPalette
    DeckStyle.NEON -> NeonPalette
    DeckStyle.SUNRISE -> SunrisePalette
}

val LocalDeck = staticCompositionLocalOf { AuroraPalette }

/** The active palette. */
val deck: DeckPalette
    @Composable get() = LocalDeck.current

/** The two-stop gradient a hue paints on tiles and headers. */
@Composable
fun hueBrush(h: Hue): Brush {
    val s = LocalDeck.current.hue(h)
    return Brush.linearGradient(listOf(s.glyph, s.glyph2))
}
