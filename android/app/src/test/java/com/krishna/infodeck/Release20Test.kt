package com.krishna.infodeck

import com.krishna.infodeck.core.DeckStyle
import com.krishna.infodeck.core.ExpiryFilter
import com.krishna.infodeck.core.FilterState
import com.krishna.infodeck.core.GreetingRules
import com.krishna.infodeck.core.HomeChip
import com.krishna.infodeck.core.HomeStatRules
import com.krishna.infodeck.core.NavRules
import com.krishna.infodeck.core.NavStyleRules
import com.krishna.infodeck.core.ReminderFilter
import com.krishna.infodeck.core.ThemeRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 2.0 — three themes (Aurora default, Neon Night dark, Sunrise custom), design 1.15.0.D1. */
class Release20Test {

    // ---------- theme resolution ----------
    @Test fun systemFollowsThePhone() {
        assertEquals(DeckStyle.AURORA, ThemeRules.resolve("SYSTEM", systemDark = false))
        assertEquals(DeckStyle.NEON, ThemeRules.resolve("SYSTEM", systemDark = true))
    }

    @Test fun lightIsAuroraWhateverThePhoneSays() {
        assertEquals(DeckStyle.AURORA, ThemeRules.resolve("LIGHT", false))
        assertEquals(DeckStyle.AURORA, ThemeRules.resolve("LIGHT", true))
    }

    @Test fun darkIsNeonWhateverThePhoneSays() {
        assertEquals(DeckStyle.NEON, ThemeRules.resolve("DARK", false))
        assertEquals(DeckStyle.NEON, ThemeRules.resolve("DARK", true))
    }

    @Test fun sunriseIsTheCustomThemeDayAndNight() {
        assertEquals(DeckStyle.SUNRISE, ThemeRules.resolve("SUNRISE", false))
        assertEquals(DeckStyle.SUNRISE, ThemeRules.resolve("SUNRISE", true))
    }

    @Test fun storedValuesFromOlderBuildsKeepTheirMeaning() {
        // 1.x stored SYSTEM / LIGHT / DARK — no migration is needed.
        assertEquals("SYSTEM", ThemeRules.normalise("SYSTEM"))
        assertEquals("LIGHT", ThemeRules.normalise("LIGHT"))
        assertEquals("DARK", ThemeRules.normalise("DARK"))
    }

    @Test fun normaliseIsCaseAndAliasTolerant() {
        assertEquals("LIGHT", ThemeRules.normalise(" light "))
        assertEquals("LIGHT", ThemeRules.normalise("aurora"))
        assertEquals("DARK", ThemeRules.normalise("Neon"))
        assertEquals("SUNRISE", ThemeRules.normalise("custom"))
        assertEquals("SUNRISE", ThemeRules.normalise("sunrise"))
    }

    @Test fun unknownOrMissingValuesNeverCrashAndFollowThePhone() {
        assertEquals("SYSTEM", ThemeRules.normalise(null))
        assertEquals("SYSTEM", ThemeRules.normalise(""))
        assertEquals("SYSTEM", ThemeRules.normalise("CANDY"))   // Option 2 was discarded
        assertEquals(DeckStyle.NEON, ThemeRules.resolve("CANDY", true))
        assertEquals(DeckStyle.AURORA, ThemeRules.resolve(null, false))
    }

    @Test fun pickerOffersExactlyFourChoicesInOrder() {
        assertEquals(listOf("SYSTEM", "LIGHT", "DARK", "SUNRISE"), ThemeRules.CHOICES.map { it.setting })
        assertEquals(listOf("System", "Aurora", "Neon Night", "Sunrise"), ThemeRules.CHOICES.map { it.title })
    }

    @Test fun everyChoiceRoundTripsThroughNormalise() {
        ThemeRules.CHOICES.forEach { assertEquals(it.setting, ThemeRules.normalise(it.setting)) }
    }

    @Test fun labelsForTheSettingsRow() {
        assertEquals("Aurora", ThemeRules.label("LIGHT"))
        assertEquals("Neon Night", ThemeRules.label("DARK"))
        assertEquals("Sunrise", ThemeRules.label("SUNRISE"))
        assertEquals("System", ThemeRules.label("garbage"))
    }

    @Test fun onlyNeonIsDark() {
        assertFalse(DeckStyle.AURORA.dark)
        assertTrue(DeckStyle.NEON.dark)
        assertFalse(DeckStyle.SUNRISE.dark)
    }

    // ---------- navigation ----------
    @Test fun fourTopLevelTabsInDesignOrder() {
        assertEquals(listOf("home", "upcoming", "tags", "settings"), NavRules.TOP_LEVEL)
        NavRules.TOP_LEVEL.forEachIndexed { i, r -> assertEquals(i, NavRules.tabIndex(r)) }
    }

    @Test fun settingsSubScreensHighlightTheSettingsTab() {
        listOf("categories", "textsize", "finetune", "errors").forEach { assertEquals(it, 3, NavRules.tabIndex(it)) }
    }

    @Test fun sunriseNeverShowsTwoNewCardButtons() {
        assertTrue(NavStyleRules.showsFloatingNew("PILL"))
        assertTrue(NavStyleRules.showsFloatingNew("GLASS"))
        assertFalse(NavStyleRules.showsFloatingNew("CENTER"))
        assertTrue(NavStyleRules.showsFloatingNew("???"))   // unknown: safe default, never hides New card
    }

    // ---------- Home hero stats keep the 1.3 chip behaviour ----------
    @Test fun overdueStatTogglesTheOverdueFilter() {
        val on = HomeStatRules.toggle(FilterState(), HomeChip.OVERDUE)
        assertEquals(ExpiryFilter.OVERDUE, on.expiry)
        assertEquals(ExpiryFilter.ANY, HomeStatRules.toggle(on, HomeChip.OVERDUE).expiry)
    }

    @Test fun monthStatTogglesThirtyDays() {
        val on = HomeStatRules.toggle(FilterState(), HomeChip.MONTH)
        assertEquals(ExpiryFilter.D30, on.expiry)
        assertEquals(ExpiryFilter.ANY, HomeStatRules.toggle(on, HomeChip.MONTH).expiry)
    }

    @Test fun noReminderStatTogglesReminderNone() {
        val on = HomeStatRules.toggle(FilterState(), HomeChip.NO_ALARM)
        assertEquals(ReminderFilter.NONE, on.reminder)
        assertEquals(ReminderFilter.ANY, HomeStatRules.toggle(on, HomeChip.NO_ALARM).reminder)
    }

    @Test fun statsSwitchExpiryButLeaveOtherFiltersAlone() {
        val start = FilterState().copy(reminder = ReminderFilter.NONE)
        val next = HomeStatRules.toggle(start, HomeChip.OVERDUE)
        assertEquals(ReminderFilter.NONE, next.reminder)
        assertEquals(ExpiryFilter.OVERDUE, next.expiry)
        // Overdue and This month share the expiry slot: picking one replaces the other.
        assertEquals(ExpiryFilter.D30, HomeStatRules.toggle(next, HomeChip.MONTH).expiry)
    }

    // ---------- Sunrise greeting ----------
    @Test fun greetingCoversEveryHour() {
        (0..23).forEach { h -> assertTrue("hour $h", GreetingRules.forHour(h).startsWith("Good")) }
        assertEquals("Good morning", GreetingRules.forHour(5))
        assertEquals("Good morning", GreetingRules.forHour(11))
        assertEquals("Good afternoon", GreetingRules.forHour(12))
        assertEquals("Good afternoon", GreetingRules.forHour(16))
        assertEquals("Good evening", GreetingRules.forHour(17))
        assertEquals("Good evening", GreetingRules.forHour(2))
    }

    @Test fun greetingOutOfRangeIsSafe() {
        assertEquals("Hello", GreetingRules.forHour(24))
        assertEquals("Hello", GreetingRules.forHour(-1))
    }
}
