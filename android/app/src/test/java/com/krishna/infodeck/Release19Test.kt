package com.krishna.infodeck

import com.krishna.infodeck.core.SearchField
import com.krishna.infodeck.core.SearchField.Kind
import com.krishna.infodeck.core.SearchRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Release19Test {
    private fun card() = listOf(
        SearchField(Kind.LABEL, "Card", "Car Insurance — HDFC Ergo"),
        SearchField(Kind.CATEGORY, "Category", "Insurance"),
        SearchField(Kind.DATA, "Policy No", "HD-2291-KX-2026"),
        SearchField(Kind.DATA, "Registration", "WB 24 AB 1234"),
        SearchField(Kind.DATA, "Garage", "Café Motors"),
        SearchField(Kind.DATE, "Policy Expiry", "11-Oct-2026 2026-10-11"),
        SearchField(Kind.FILE, "File", "Policy document.pdf"),
        SearchField(Kind.TAG, "Tag", "renewal"),
        SearchField(Kind.REMINDER, "Reminder", "30 days before Policy Expiry")
    )
    private val idx = SearchRules.index(card())

    // ---------- B11: every field kind lands in the index ----------
    @Test fun secondDataVariableIsSearchable() = assertTrue(SearchRules.matches(idx, "WB 24"))
    @Test fun dateTextIsSearchable() { assertTrue(SearchRules.matches(idx, "oct")); assertTrue(SearchRules.matches(idx, "11-oct-2026")); assertTrue(SearchRules.matches(idx, "2026-10-11")) }
    @Test fun fileNameIsSearchable() = assertTrue(SearchRules.matches(idx, "document.pdf"))
    @Test fun reminderRuleIsSearchable() = assertTrue(SearchRules.matches(idx, "30 days before"))
    @Test fun tagAndCategoryStillSearchable() { assertTrue(SearchRules.matches(idx, "renewal")); assertTrue(SearchRules.matches(idx, "insurance")) }
    @Test fun dataNamesAreSearchableToo() = assertTrue(SearchRules.matches(idx, "garage"))

    // ---------- matching rules ----------
    @Test fun caseDoesNotMatter() = assertTrue(SearchRules.matches(idx, "hdfc"))
    @Test fun partialIsFine() = assertTrue(SearchRules.matches(idx, "229"))
    @Test fun separatorBlindCodes() { assertTrue(SearchRules.matches(idx, "wb24ab")); assertTrue(SearchRules.matches(idx, "hd2291kx")) }
    @Test fun accentsFold() = assertTrue(SearchRules.matches(idx, "cafe"))
    @Test fun everyWordMustMatchSomewhere() { assertTrue(SearchRules.matches(idx, "hdfc 2291")); assertFalse(SearchRules.matches(idx, "hdfc 9999")) }
    @Test fun blankQueryMatchesEverything() { assertTrue(SearchRules.matches(idx, "")); assertTrue(SearchRules.matches(idx, "   ")) }
    @Test fun queryIsTrimmedAtTheLimit() = assertEquals(SearchRules.words("a".repeat(500)).first().length, SearchRules.MAX_QUERY)
    @Test fun nonsenseDoesNotMatch() = assertFalse(SearchRules.matches(idx, "zzqx"))

    // ---------- row hint ----------
    @Test fun hintShowsTheMatchingFieldWithRange() {
        val h = SearchRules.hit(card(), "2291")
        assertNotNull(h); assertEquals("Policy No", h!!.field.name); assertEquals(3, h.start); assertEquals(7, h.end)
    }
    @Test fun labelHitNeedsNoHint() = assertNull(SearchRules.hit(card(), "hdfc"))
    @Test fun hintPrefersDataOverDate() = assertEquals(Kind.DATA, SearchRules.hit(card(), "2026")!!.field.kind)
    @Test fun rankingLabelFirst() { assertEquals(0, SearchRules.rank(card(), "hdfc")); assertEquals(1, SearchRules.rank(card(), "2291")); assertEquals(3, SearchRules.rank(card(), "document")) }

    // ---------- empty state and buckets ----------
    @Test fun missingWordsAreNamed() = assertEquals(listOf("9999"), SearchRules.missing(listOf(idx), "hdfc 9999"))
    @Test fun kindsHitDriveTheBuckets() {
        val k = SearchRules.kindsHit(card(), "2026")
        assertTrue(k.contains(Kind.DATA)); assertTrue(k.contains(Kind.DATE))
    }
    @Test fun indexNeverThrowsOnEmptyFields() = assertEquals("", SearchRules.index(emptyList()))
}
