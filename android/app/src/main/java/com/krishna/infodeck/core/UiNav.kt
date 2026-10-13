package com.krishna.infodeck.core

/** Home's compact filter strip (design 1.3.D1 C2). */
enum class HomeChip(val label: String) {
    OVERDUE("Overdue"), MONTH("Month"), NO_ALARM("No alarm")
}

object ChipRules {
    /** A chip whose count is zero (or negative, which would mean a bad query) is not drawn. */
    fun visible(counts: Map<HomeChip, Int>): List<HomeChip> {
        val bad = counts.filterValues { it < 0 }
        if (bad.isNotEmpty()) Logger.e("ChipRules", "negative counts: " + bad.keys.joinToString())
        return HomeChip.values().filter { (counts[it] ?: 0) > 0 }
    }
}

/** Where the slim bottom bar shows (design 1.3.D1 C3). */
object NavRules {
    val TOP_LEVEL = listOf("home", "upcoming", "settings")

    /** Visible on every top-level and Settings sub-screen; hidden where an action bar owns the bottom. */
    fun barVisible(route: String?): Boolean {
        val r = route?.substringBefore('?')?.substringBefore('/') ?: return true
        return when (r) {
            "home", "upcoming", "settings", "categories", "tags", "textsize", "finetune", "errors" -> true
            "editor", "card" -> false
            else -> {
                Logger.e("NavRules", "unknown route '" + r + "', showing the bar")
                true
            }
        }
    }

    fun tabIndex(route: String?): Int = when (route?.substringBefore('?')?.substringBefore('/')) {
        "upcoming" -> 1
        "settings", "categories", "tags", "textsize", "finetune", "errors" -> 2
        else -> 0
    }
}

/** Settings is eight collapsible groups, one open at a time (design 1.3.D1 C5). */
enum class SettingsGroup(val label: String) {
    ORGANISE("Organise"),
    LOOK("Look and feel"),
    REMINDERS("Reminders"),
    TYPING("Typing"),
    DATA("Data and backup"),
    SECURITY("Security"),
    DIAGNOSTICS("Diagnostics and About"),
    DANGER("Danger zone")
}

data class SettingsRow(val title: String, val group: SettingsGroup, val keywords: String = "")

object SettingsIndex {
    val ROWS: List<SettingsRow> = listOf(
        SettingsRow("Categories", SettingsGroup.ORGANISE, "category folder group colour"),
        SettingsRow("Tags", SettingsGroup.ORGANISE, "tag label merge rename"),
        SettingsRow("Default category for new cards", SettingsGroup.ORGANISE, "plus new card default"),
        SettingsRow("Group Home by", SettingsGroup.ORGANISE, "grouping section category tag"),
        SettingsRow("Text size", SettingsGroup.LOOK, "font bigger smaller readable size"),
        SettingsRow("Theme", SettingsGroup.LOOK, "dark light system colour"),
        SettingsRow("One section open at a time", SettingsGroup.LOOK, "accordion collapse"),
        SettingsRow("Default ring time", SettingsGroup.REMINDERS, "alarm clock hour"),
        SettingsRow("Vibrate", SettingsGroup.REMINDERS, "buzz haptic"),
        SettingsRow("Snooze length", SettingsGroup.REMINDERS, "later postpone"),
        SettingsRow("Test ring", SettingsGroup.REMINDERS, "sample notification"),
        SettingsRow("Notifications permission", SettingsGroup.REMINDERS, "permission allow blocked"),
        SettingsRow("Exact alarms", SettingsGroup.REMINDERS, "permission exact schedule"),
        SettingsRow("Auto-capitalise words", SettingsGroup.TYPING, "caps capital keyboard"),
        SettingsRow("Include data values", SettingsGroup.TYPING, "caps scope values"),
        SettingsRow("Backup to file", SettingsGroup.DATA, "export json save"),
        SettingsRow("Restore from file", SettingsGroup.DATA, "import json merge replace"),
        SettingsRow("Attached files", SettingsGroup.DATA, "pdf word storage size"),
        SettingsRow("App lock", SettingsGroup.SECURITY, "biometric fingerprint pin lock"),
        SettingsRow("Error Logs", SettingsGroup.DIAGNOSTICS, "log diagnostics crash"),
        SettingsRow("About Me", SettingsGroup.DIAGNOSTICS, "version release contact krishna"),
        SettingsRow("Put settings back to normal", SettingsGroup.DANGER, "reset defaults"),
        SettingsRow("Erase everything", SettingsGroup.DANGER, "delete wipe erase reset")
    )

    /** Blank query returns nothing (the groups stay collapsed); queries are trimmed at 100 chars. */
    fun search(query: String): List<SettingsRow> {
        val q = query.trim().take(100).lowercase()
        if (q.isEmpty()) return emptyList()
        return ROWS.filter { it.title.lowercase().contains(q) || it.keywords.contains(q) }
    }

    fun rowsIn(group: SettingsGroup): List<SettingsRow> = ROWS.filter { it.group == group }
}
