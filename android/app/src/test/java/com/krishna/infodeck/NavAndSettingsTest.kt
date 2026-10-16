package com.krishna.infodeck

import com.krishna.infodeck.core.ChipRules
import com.krishna.infodeck.core.HomeChip
import com.krishna.infodeck.core.NavRules
import com.krishna.infodeck.core.SettingsGroup
import com.krishna.infodeck.core.SettingsIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavAndSettingsTest {

    // ---------- N40: the compact filter strip ----------
    @Test fun zeroCountChipsAreHidden() {
        val out = ChipRules.visible(mapOf(HomeChip.OVERDUE to 0, HomeChip.MONTH to 3, HomeChip.NO_ALARM to 5))
        assertEquals(listOf(HomeChip.MONTH, HomeChip.NO_ALARM), out)
    }

    @Test fun allZeroLeavesAnEmptyStrip() =
        assertTrue(ChipRules.visible(HomeChip.values().associateWith { 0 }).isEmpty())

    @Test fun missingCountCountsAsZero() =
        assertEquals(listOf(HomeChip.OVERDUE), ChipRules.visible(mapOf(HomeChip.OVERDUE to 2)))

    @Test fun negativeCountsAreDroppedNotDrawn() =
        assertTrue(ChipRules.visible(mapOf(HomeChip.OVERDUE to -4)).isEmpty())

    @Test fun chipOrderIsStable() {
        val out = ChipRules.visible(HomeChip.values().associateWith { 1 })
        assertEquals(HomeChip.values().toList(), out)
    }

    // ---------- N41: where the bottom bar shows ----------
    @Test fun barShowsOnEveryTopLevelScreen() =
        NavRules.TOP_LEVEL.forEach { assertTrue(it, NavRules.barVisible(it)) }

    @Test fun barShowsOnSettingsSubScreens() =
        listOf("categories", "tags", "textsize", "finetune", "errors")
            .forEach { assertTrue(it, NavRules.barVisible(it)) }

    @Test fun barHidesWhereAnActionBarOwnsTheBottom() {
        assertFalse(NavRules.barVisible("editor?cardId=3&catId=0"))
        assertFalse(NavRules.barVisible("card/12"))
    }

    @Test fun unknownRouteFallsBackToShowingTheBar() = assertTrue(NavRules.barVisible("nonsense"))

    @Test fun nullRouteIsSafe() = assertTrue(NavRules.barVisible(null))

    @Test fun tabIndexFollowsTheRoute() {
        assertEquals(0, NavRules.tabIndex("home"))
        assertEquals(1, NavRules.tabIndex("upcoming"))
        // 2.0: Home · Upcoming · Tags · Settings (design 1.15.0.D1)
        assertEquals(2, NavRules.tabIndex("tags"))
        assertEquals(3, NavRules.tabIndex("settings"))
        assertEquals(3, NavRules.tabIndex("categories"))
        assertEquals(3, NavRules.tabIndex("errors"))
        assertEquals(0, NavRules.tabIndex(null))
    }

    // ---------- N43: settings groups ----------
    @Test fun everyRowBelongsToExactlyOneGroup() {
        val counted = SettingsGroup.values().sumOf { g -> SettingsIndex.rowsIn(g).size }
        assertEquals(SettingsIndex.ROWS.size, counted)
    }

    @Test fun everyGroupHoldsSomething() =
        SettingsGroup.values().forEach { assertTrue(it.label, SettingsIndex.rowsIn(it).isNotEmpty()) }

    @Test fun noRowTitleIsDuplicated() =
        assertEquals(SettingsIndex.ROWS.size, SettingsIndex.ROWS.map { it.title }.toSet().size)

    @Test fun categoriesAndTagsMovedIntoOrganise() {
        val organise = SettingsIndex.rowsIn(SettingsGroup.ORGANISE).map { it.title }
        assertTrue(organise.contains("Categories"))
        assertTrue(organise.contains("Tags"))
    }

    @Test fun dangerHoldsBothResets() {
        val danger = SettingsIndex.rowsIn(SettingsGroup.DANGER).map { it.title }
        assertTrue(danger.contains("Put settings back to normal"))
        assertTrue(danger.contains("Erase everything"))
    }

    // ---------- N42: settings search ----------
    @Test fun blankQueryMatchesNothing() {
        assertTrue(SettingsIndex.search("").isEmpty())
        assertTrue(SettingsIndex.search("    ").isEmpty())
    }

    @Test fun searchFindsByTitle() =
        assertTrue(SettingsIndex.search("text size").any { it.title == "Text size" })

    @Test fun searchFindsByKeyword() {
        assertTrue(SettingsIndex.search("font").any { it.title == "Text size" })
        assertTrue(SettingsIndex.search("fingerprint").any { it.title == "App lock" })
        assertTrue(SettingsIndex.search("export").any { it.title == "Backup to file" })
    }

    @Test fun searchIsCaseInsensitive() =
        assertEquals(SettingsIndex.search("BACKUP").size, SettingsIndex.search("backup").size)

    @Test fun longQueriesAreTrimmedNotCrashed() =
        assertTrue(SettingsIndex.search("x".repeat(500)).isEmpty())

    @Test fun everyHitCarriesItsGroup() =
        SettingsIndex.search("a").forEach { assertTrue(SettingsIndex.rowsIn(it.group).contains(it)) }
}
