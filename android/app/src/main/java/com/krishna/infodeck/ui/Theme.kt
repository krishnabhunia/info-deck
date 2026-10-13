package com.krishna.infodeck.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Palette locked in the D1/D2 designs.
val Primary = Color(0xFF2F4FCD)
val PrimaryDark = Color(0xFF1E357F)
val PrimaryContainer = Color(0xFFE7EBFF)
val OnPrimaryContainer = Color(0xFF22337F)
val Amber = Color(0xFFA9700D)
val AmberContainer = Color(0xFFFDF2DC)
val Ok = Color(0xFF1E8E5A)
val OkContainer = Color(0xFFE3F5EC)
val Danger = Color(0xFFC0392F)
val DangerContainer = Color(0xFFFCEAE8)
val Ink = Color(0xFF131A26)
val Ink2 = Color(0xFF5A6675)
val Hint = Color(0xFF96A0AF)
val Bg = Color(0xFFF5F7FB)
val Surface = Color(0xFFFFFFFF)
val LineColor = Color(0xFFE6EAF2)

/** The 8 fixed category colours (D2 S8.4). */
val PrimaryViolet = Color(0xFF7A5AF8)
val PrimaryMid = Color(0xFF5A6BF0)

/** The brand gradient used by the hero, the FAB and the Save button. */
val BrandGradient: androidx.compose.ui.graphics.Brush
    get() = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Primary, PrimaryMid, PrimaryViolet))

val CategoryColors = listOf(
    Color(0xFF4A6FB0), Color(0xFF2E8A86), Color(0xFF4E9440), Color(0xFFD48A18),
    Color(0xFF7B57C0), Color(0xFFC2528A), Color(0xFFC94A42), Color(0xFF7B8794)
)

fun categoryColor(index: Int): Color = CategoryColors[index.coerceIn(0, CategoryColors.lastIndex)]

private val LightScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Amber,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = Amber,
    tertiary = Ok,
    tertiaryContainer = OkContainer,
    onTertiaryContainer = Ok,
    background = Bg,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDF0F4),
    onSurfaceVariant = Ink2,
    outline = Color(0xFFB7BFCC),
    outlineVariant = LineColor,
    error = Danger,
    onError = Color.White,
    errorContainer = DangerContainer,
    onErrorContainer = Danger
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9CB6E8),
    onPrimary = Color(0xFF0E2148),
    primaryContainer = Color(0xFF2A4272),
    onPrimaryContainer = Color(0xFFD9E3F7),
    secondary = Color(0xFFE9BC72),
    secondaryContainer = Color(0xFF4A3512),
    onSecondaryContainer = Color(0xFFFFE2B4),
    tertiary = Color(0xFF9BD49A),
    tertiaryContainer = Color(0xFF1E3F20),
    onTertiaryContainer = Color(0xFFCDEBCB),
    background = Color(0xFF12161C),
    onBackground = Color(0xFFE6EAF0),
    surface = Color(0xFF1A1F27),
    onSurface = Color(0xFFE6EAF0),
    surfaceVariant = Color(0xFF262D37),
    onSurfaceVariant = Color(0xFFB5BECB),
    outline = Color(0xFF6C7684),
    outlineVariant = Color(0xFF333B46),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

@Composable
fun InfoDeckTheme(
    themeSetting: String,
    ui: com.krishna.infodeck.core.UiScale = com.krishna.infodeck.core.UiScale(),
    followSystemText: Boolean = true,
    content: @Composable () -> Unit
) {
    val dark = when (themeSetting) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }
    val scheme = if (dark) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        val context = LocalContext.current
        SideEffect {
            (context as? Activity)?.window?.let { w ->
                w.statusBarColor = (if (dark) Color(0xFF12161C) else Primary).toArgb()
                WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = false
            }
        }
    }
    // One value drives every sp in the app: the device density is re-provided with
    // InfoDeck's own font scale, optionally multiplied by the phone's setting.
    val base = androidx.compose.ui.platform.LocalDensity.current
    val effective = if (followSystemText) base.fontScale * ui.textScale else ui.textScale
    val density = androidx.compose.ui.unit.Density(density = base.density, fontScale = effective)

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides density,
        com.krishna.infodeck.core.LocalUiScale provides ui
    ) {
        MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
    }
}
