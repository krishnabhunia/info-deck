package com.krishna.infodeck.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat
import com.krishna.infodeck.core.Hue
import com.krishna.infodeck.core.Hues
import com.krishna.infodeck.core.Logger
import com.krishna.infodeck.core.ThemeRules

// 2.0 — the old fixed palette is gone. These names stay so every screen keeps compiling,
// but each now reads the active theme (Aurora, Neon Night or Sunrise Wallet).
val Primary: Color @Composable get() = deck.hue(Hue.INDIGO).glyph
val PrimaryDark: Color @Composable get() = deck.hue(Hue.INDIGO).onTint
val PrimaryContainer: Color @Composable get() = deck.hue(Hue.INDIGO).tint
val OnPrimaryContainer: Color @Composable get() = deck.hue(Hue.INDIGO).onTint
val Amber: Color @Composable get() = deck.hue(Hue.AMBER).onTint
val AmberContainer: Color @Composable get() = deck.hue(Hue.AMBER).tint
val Ok: Color @Composable get() = deck.hue(Hue.GREEN).onTint
val OkContainer: Color @Composable get() = deck.hue(Hue.GREEN).tint
val Danger: Color @Composable get() = deck.hue(Hue.ROSE).glyph
val DangerContainer: Color @Composable get() = deck.hue(Hue.ROSE).tint
val Ink: Color @Composable get() = deck.ink
val Ink2: Color @Composable get() = deck.ink2
val Hint: Color @Composable get() = deck.hint
val Bg: Color @Composable get() = deck.bg
val Surface: Color @Composable get() = deck.surface
val LineColor: Color @Composable get() = deck.line
val PrimaryViolet: Color @Composable get() = deck.brand.last()
val PrimaryMid: Color @Composable get() = deck.brand[deck.brand.size / 2]

/** The brand gradient used by the hero, the FAB and the Save button. */
val BrandGradient: Brush
    @Composable get() = Brush.linearGradient(deck.brand)

/** The 8 category colours, in the active theme. A category's dot and its tile always agree. */
val CategoryColors: List<Color>
    @Composable get() = (0 until 8).map { deck.hue(Hues.forCategory(it)).glyph }

@Composable
fun categoryColor(index: Int): Color {
    if (index < 0) Logger.e("Theme", "negative category colour index $index")
    return deck.hue(Hues.forCategory(index.coerceIn(0, 7))).glyph
}

private fun schemeFor(p: DeckPalette): ColorScheme {
    val pri = p.hue(Hue.INDIGO)
    val amber = p.hue(Hue.AMBER)
    val green = p.hue(Hue.GREEN)
    val rose = p.hue(Hue.ROSE)
    val onPri = if (pri.glyph.luminance() > 0.5f) Color(0xFF0A0E1F) else Color.White
    return if (p.dark) darkColorScheme(
        primary = pri.glyph, onPrimary = onPri,
        primaryContainer = pri.tint, onPrimaryContainer = pri.onTint,
        secondary = amber.glyph, onSecondary = Color(0xFF0A0E1F),
        secondaryContainer = amber.tint, onSecondaryContainer = amber.onTint,
        tertiary = green.glyph, onTertiary = Color(0xFF0A0E1F),
        tertiaryContainer = green.tint, onTertiaryContainer = green.onTint,
        background = p.bg, onBackground = p.ink,
        surface = p.surface, onSurface = p.ink,
        surfaceVariant = p.surface2, onSurfaceVariant = p.ink2,
        surfaceTint = Color.Transparent,
        outline = p.outline, outlineVariant = p.line,
        error = rose.glyph, onError = Color(0xFF0A0E1F),
        errorContainer = rose.tint, onErrorContainer = rose.onTint
    ) else lightColorScheme(
        primary = pri.glyph, onPrimary = onPri,
        primaryContainer = pri.tint, onPrimaryContainer = pri.onTint,
        secondary = amber.onTint, onSecondary = Color.White,
        secondaryContainer = amber.tint, onSecondaryContainer = amber.onTint,
        tertiary = green.onTint, onTertiary = Color.White,
        tertiaryContainer = green.tint, onTertiaryContainer = green.onTint,
        background = p.bg, onBackground = p.ink,
        surface = p.surface, onSurface = p.ink,
        surfaceVariant = p.surface2, onSurfaceVariant = p.ink2,
        surfaceTint = Color.Transparent,
        outline = p.outline, outlineVariant = p.line,
        error = rose.onTint, onError = Color.White,
        errorContainer = rose.tint, onErrorContainer = rose.onTint
    )
}

private fun typographyFor(p: DeckPalette): Typography {
    val t = Typography()
    fun TextStyle.body() = copy(fontFamily = p.body)
    fun TextStyle.head() = copy(fontFamily = p.heading)
    return Typography(
        displayLarge = t.displayLarge.head(), displayMedium = t.displayMedium.head(), displaySmall = t.displaySmall.head(),
        headlineLarge = t.headlineLarge.head(), headlineMedium = t.headlineMedium.head(), headlineSmall = t.headlineSmall.head(),
        titleLarge = t.titleLarge.head(), titleMedium = t.titleMedium.body(), titleSmall = t.titleSmall.body(),
        bodyLarge = t.bodyLarge.body(), bodyMedium = t.bodyMedium.body(), bodySmall = t.bodySmall.body(),
        labelLarge = t.labelLarge.body(), labelMedium = t.labelMedium.body(), labelSmall = t.labelSmall.body()
    )
}

/**
 * Paints the system status bar to match the band behind it. Every screen header calls this,
 * so the bar always continues the header's colour.
 */
@Composable
fun StatusBarColour(colour: Color) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val context = LocalContext.current
    SideEffect {
        try {
            (context as? Activity)?.window?.let { w ->
                @Suppress("DEPRECATION")
                w.statusBarColor = colour.toArgb()
                WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = colour.luminance() > 0.6f
            }
        } catch (t: Throwable) {
            Logger.e("Theme", "could not paint the status bar", t)
        }
    }
}

@Composable
fun InfoDeckTheme(
    themeSetting: String,
    ui: com.krishna.infodeck.core.UiScale = com.krishna.infodeck.core.UiScale(),
    followSystemText: Boolean = true,
    content: @Composable () -> Unit
) {
    val style = ThemeRules.resolve(themeSetting, isSystemInDarkTheme())
    val palette = paletteFor(style)

    // One value drives every sp in the app: the device density is re-provided with
    // InfoDeck's own font scale, optionally multiplied by the phone's setting.
    val base = androidx.compose.ui.platform.LocalDensity.current
    val effective = if (followSystemText) base.fontScale * ui.textScale else ui.textScale
    val density = androidx.compose.ui.unit.Density(density = base.density, fontScale = effective)

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides density,
        com.krishna.infodeck.core.LocalUiScale provides ui,
        LocalDeck provides palette
    ) {
        StatusBarColour(if (!palette.dark && palette.heroOnColour) palette.brand.first() else palette.bg)
        MaterialTheme(colorScheme = schemeFor(palette), typography = typographyFor(palette), content = content)
    }
}
