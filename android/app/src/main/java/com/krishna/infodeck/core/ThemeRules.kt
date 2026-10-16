package com.krishna.infodeck.core

/**
 * 2.0 — the three looks (design 1.15.0.D1, approved 15-Oct-2026).
 *
 *  - AURORA  — Option 1, the default light theme
 *  - NEON    — Option 3, the dark theme
 *  - SUNRISE — Option 4, the custom theme the user can pick
 *
 * The stored setting keeps its old values so nothing needs migrating:
 * SYSTEM follows the phone (Aurora by day, Neon at night), LIGHT is Aurora,
 * DARK is Neon, and the new SUNRISE value picks the custom theme.
 */
enum class DeckStyle(val label: String, val blurb: String, val dark: Boolean) {
    AURORA("Aurora", "Default · light", false),
    NEON("Neon Night", "Dark", true),
    SUNRISE("Sunrise Wallet", "Custom", false)
}

data class ThemeChoice(val setting: String, val title: String, val caption: String)

object ThemeRules {
    const val SYSTEM = "SYSTEM"
    const val LIGHT = "LIGHT"
    const val DARK = "DARK"
    const val SUNRISE = "SUNRISE"

    /** The picker, in the order it is shown. */
    val CHOICES: List<ThemeChoice> = listOf(
        ThemeChoice(SYSTEM, "System", "Aurora by day, Neon at night"),
        ThemeChoice(LIGHT, "Aurora", "Default · light"),
        ThemeChoice(DARK, "Neon Night", "Dark"),
        ThemeChoice(SUNRISE, "Sunrise", "Custom · warm wallet")
    )

    /** A stored value (any case, possibly from an older build or a backup) to the canonical one. */
    fun normalise(setting: String?): String {
        val s = setting?.trim()?.uppercase().orEmpty()
        return when (s) {
            SYSTEM, "" -> SYSTEM
            LIGHT, "AURORA" -> LIGHT
            DARK, "NEON" -> DARK
            SUNRISE, "CUSTOM" -> SUNRISE
            else -> {
                Logger.e("ThemeRules", "unknown theme setting '$setting', using System")
                SYSTEM
            }
        }
    }

    /** Which look to draw. Unknown values never crash — they follow the phone. */
    fun resolve(setting: String?, systemDark: Boolean): DeckStyle = when (normalise(setting)) {
        LIGHT -> DeckStyle.AURORA
        DARK -> DeckStyle.NEON
        SUNRISE -> DeckStyle.SUNRISE
        else -> if (systemDark) DeckStyle.NEON else DeckStyle.AURORA
    }

    /** What the Settings row shows next to "Theme". */
    fun label(setting: String?): String =
        CHOICES.firstOrNull { it.setting == normalise(setting) }?.title ?: "System"
}

/** Sunrise's bottom bar carries the New-card button, so it never floats a second one. */
object NavStyleRules {
    fun showsFloatingNew(navStyle: String): Boolean = when (navStyle) {
        "PILL", "GLASS" -> true
        "CENTER" -> false
        else -> {
            Logger.e("NavStyleRules", "unknown nav style '$navStyle', showing the floating button")
            true
        }
    }
}

/** "Good morning / afternoon / evening" for the Sunrise greeting, from the phone clock. */
object GreetingRules {
    fun forHour(hour: Int): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..23, in 0..4 -> "Good evening"
        else -> {
            Logger.e("GreetingRules", "hour out of range: $hour")
            "Hello"
        }
    }

    fun now(): String = forHour(java.time.LocalTime.now().hour)
}
