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

/** 2.0 — a Home hero stat toggles exactly the filter its 1.3 chip did (design 1.15.0.D1 frame 02). */
object HomeStatRules {
    fun toggle(f: FilterState, chip: HomeChip): FilterState = when (chip) {
        HomeChip.OVERDUE -> f.copy(expiry = if (f.expiry == ExpiryFilter.OVERDUE) ExpiryFilter.ANY else ExpiryFilter.OVERDUE)
        HomeChip.MONTH -> f.copy(expiry = if (f.expiry == ExpiryFilter.D30) ExpiryFilter.ANY else ExpiryFilter.D30)
        HomeChip.NO_ALARM -> f.copy(reminder = if (f.reminder == ReminderFilter.NONE) ReminderFilter.ANY else ReminderFilter.NONE)
    }
}

/** Where the bottom bar shows (design 1.3.D1 C3; 2.0 adds the Tags tab — design 1.15.0.D1). */
object NavRules {
    val TOP_LEVEL = listOf("home", "upcoming", "tags", "settings")

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
        "tags" -> 2
        "settings", "categories", "textsize", "finetune", "errors" -> 3
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
    DIAGNOSTICS("Diagnostics and updates"),
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
        SettingsRow("Check for updates", SettingsGroup.DIAGNOSTICS, "github update version install release"),
        SettingsRow("Include beta updates", SettingsGroup.DIAGNOSTICS, "beta prerelease early access opt in"),
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
