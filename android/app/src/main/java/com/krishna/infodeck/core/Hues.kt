package com.krishna.infodeck.core

/**
 * The nine-hue colour system (design 1.5.D1 S4). A hue is never decorative:
 * amber always means time, rose always means trouble, purple always means a file.
 */
enum class Hue(val argb: Long, val tint: Long, val onTint: Long, val label: String) {
    INDIGO(0xFF3B5BDB, 0xFFE8ECFD, 0xFF2B44A8, "Indigo"),
    AMBER(0xFFB76E00, 0xFFFDF0DA, 0xFF8A5300, "Amber"),
    ROSE(0xFFE5484D, 0xFFFDE7E8, 0xFFB32B30, "Rose"),
    GREEN(0xFF12A150, 0xFFDFF5E8, 0xFF0B6B36, "Green"),
    VIOLET(0xFF7A5AF8, 0xFFEEE9FE, 0xFF5A3ED0, "Violet"),
    PURPLE(0xFF9333EA, 0xFFF3E8FE, 0xFF6D22B0, "Purple"),
    TEAL(0xFF0E9384, 0xFFDDF3F0, 0xFF0A6B60, "Teal"),
    SKY(0xFF0284C7, 0xFFE0F2FE, 0xFF02648F, "Sky"),
    SLATE(0xFF64748B, 0xFFEEF1F5, 0xFF48566B, "Slate");

    /** Dark theme: the same hue lifted two steps on a #1D2431 surface. */
    val darkGlyph: Long get() = lift(argb)
    val darkTint: Long get() = (argb and 0x00FFFFFF) or 0x38000000

    private fun lift(c: Long): Long {
        val r = ((c shr 16) and 0xFF).toInt()
        val g = ((c shr 8) and 0xFF).toInt()
        val b = (c and 0xFF).toInt()
        fun up(v: Int) = (v + (255 - v) * 45 / 100).coerceIn(0, 255).toLong()
        return 0xFF000000 or (up(r) shl 16) or (up(g) shl 8) or up(b)
    }
}

object Hues {
    /** What each thing in the app is coloured by. */
    fun forRole(role: String): Hue = when (role.lowercase()) {
        "home", "edit", "primary", "nav" -> Hue.INDIGO
        "reminder", "alarm", "upcoming", "time", "warranty" -> Hue.AMBER
        "overdue", "danger", "delete", "error" -> Hue.ROSE
        "share", "ok", "healthy", "date" -> Hue.GREEN
        "qr", "tag" -> Hue.VIOLET
        "file", "attachment" -> Hue.PURPLE
        "settings", "copy", "identity" -> Hue.TEAL
        "category", "data", "backup" -> Hue.SKY
        "quiet", "disabled", "uncategorised", "more" -> Hue.SLATE
        else -> {
            Logger.e("Hues", "unknown role '$role', falling back to slate")
            Hue.SLATE
        }
    }

    /** A stable hue per category, so a card's tile and its chip always agree. */
    fun forCategory(index: Int): Hue {
        if (index < 0) {
            Logger.e("Hues", "negative category index $index")
            return Hue.SLATE
        }
        val wheel = listOf(Hue.INDIGO, Hue.TEAL, Hue.AMBER, Hue.GREEN, Hue.SKY, Hue.VIOLET, Hue.PURPLE, Hue.ROSE)
        return wheel[index % wheel.size]
    }

    /** N56 — a glyph key per category, guessed from its name, overridable later. */
    fun glyphFor(categoryName: String): String {
        val n = categoryName.lowercase()
        return when {
            n.contains("insur") || n.contains("policy") -> "shield"
            n.contains("vehicle") || n.contains("car") || n.contains("bike") -> "car"
            n.contains("warrant") || n.contains("device") || n.contains("gadget") -> "laptop"
            n.contains("id") || n.contains("identity") || n.contains("passport") -> "id"
            n.contains("bank") || n.contains("card") || n.contains("account") -> "wallet"
            n.contains("health") || n.contains("medic") -> "health"
            n.contains("home") || n.contains("wifi") || n.contains("net") -> "wifi"
            n.contains("subscri") || n.contains("bill") -> "receipt"
            n.isBlank() -> "card"
            else -> "card"
        }
    }
}
