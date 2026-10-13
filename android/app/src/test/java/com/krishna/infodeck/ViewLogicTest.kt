package com.krishna.infodeck

import com.krishna.infodeck.core.CardRow
import com.krishna.infodeck.core.CategoryRow
import com.krishna.infodeck.core.ContentFilter
import com.krishna.infodeck.core.ExpiryFilter
import com.krishna.infodeck.core.FilterState
import com.krishna.infodeck.core.GroupBy
import com.krishna.infodeck.core.GroupOrder
import com.krishna.infodeck.core.ReminderFilter
import com.krishna.infodeck.core.SortBy
import com.krishna.infodeck.core.ViewLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ViewLogicTest {

    private val today = LocalDate.of(2026, 8, 18)

    private fun row(
        id: Long, label: String, catId: Long = 1, catName: String = "Insurance",
        key: LocalDate? = null, ring: LocalDateTime? = null, hasData: Boolean = true,
        snoozed: Boolean = false, off: Boolean = false, updated: Long = id, created: Long = id, order: Int = id.toInt()
    ) = CardRow(
        id = id, label = label, categoryId = catId, categoryName = catName, colorIndex = 0,
        keyDateName = key?.let { "Expiry" }, keyDate = key,
        firstDataName = if (hasData) "Number" else null, firstDataValue = if (hasData) "X" else null,
        hasData = hasData, hasDate = key != null,
        nextRingAt = ring, nextRingRule = ring?.let { "rule" },
        hasReminder = ring != null || snoozed, anySnoozed = snoozed, anyOff = off,
        manualOrder = order, createdAt = created, updatedAt = updated
    )

    private val carInsurance = row(1, "Car insurance", 1, "Insurance", today.plusDays(54), LocalDateTime.of(2026, 9, 11, 9, 0))
    private val bikeInsurance = row(2, "Bike insurance", 1, "Insurance", today.minusDays(310))
    private val passport = row(3, "Passport", 2, "Documents", today.plusDays(1000))
    private val laptop = row(4, "Laptop warranty", 3, "Warranties", today.plusDays(20), LocalDateTime.of(2026, 8, 25, 9, 0))
    private val wifi = row(5, "Wi-Fi password", 4, "Uncategorised", null, null)
    private val all = listOf(carInsurance, bikeInsurance, passport, laptop, wifi)

    private val categories = listOf(
        CategoryRow(1, "Insurance", 0, 0, false),
        CategoryRow(2, "Documents", 1, 1, false),
        CategoryRow(3, "Warranties", 2, 2, false),
        CategoryRow(4, "Uncategorised", 7, 100000, true)
    )

    // ---------------- filters ----------------
    @Test fun noFilterKeepsEverything() =
        assertEquals(5, ViewLogic.filter(all, FilterState(), today).size)

    @Test fun categoryFilterKeepsOnlyThatCategory() {
        val out = ViewLogic.filter(all, FilterState(categoryIds = setOf(1L)), today)
        assertEquals(listOf(1L, 2L), out.map { it.id })
    }

    @Test fun overdueFilterKeepsOnlyPastKeyDates() {
        val out = ViewLogic.filter(all, FilterState(expiry = ExpiryFilter.OVERDUE), today)
        assertEquals(listOf(2L), out.map { it.id })
    }

    @Test fun thirtyDayWindowExcludesTodayPlus54AndPastDates() {
        val out = ViewLogic.filter(all, FilterState(expiry = ExpiryFilter.D30), today)
        assertEquals(listOf(4L), out.map { it.id })
    }

    @Test fun expiryFilterDropsCardsWithoutAKeyDate() {
        val out = ViewLogic.filter(all, FilterState(expiry = ExpiryFilter.M12), today)
        assertFalse(out.any { it.id == 5L })
    }

    @Test fun customRangeIsInclusiveOnBothEnds() {
        val f = FilterState(
            expiry = ExpiryFilter.CUSTOM,
            customFrom = today.toEpochDay(),
            customTo = today.plusDays(54).toEpochDay()
        )
        assertEquals(listOf(1L, 4L), ViewLogic.filter(all, f, today).map { it.id }.sorted())
    }

    @Test fun reminderFiltersSplitHasAndNone() {
        assertEquals(listOf(1L, 4L), ViewLogic.filter(all, FilterState(reminder = ReminderFilter.HAS), today).map { it.id })
        assertEquals(listOf(2L, 3L, 5L), ViewLogic.filter(all, FilterState(reminder = ReminderFilter.NONE), today).map { it.id })
    }

    @Test fun contentFiltersSplitDatedAndUndated() {
        assertEquals(listOf(1L, 2L, 3L, 4L), ViewLogic.filter(all, FilterState(content = ContentFilter.HAS_DATE), today).map { it.id })
        assertEquals(listOf(5L), ViewLogic.filter(all, FilterState(content = ContentFilter.NO_DATE), today).map { it.id })
    }

    @Test fun filtersCombineAsAnd() {
        val f = FilterState(categoryIds = setOf(1L), expiry = ExpiryFilter.OVERDUE)
        assertEquals(listOf(2L), ViewLogic.filter(all, f, today).map { it.id })
    }

    @Test fun activeGroupCountAndFlag() {
        val f = FilterState(categoryIds = setOf(1L), expiry = ExpiryFilter.D7, reminder = ReminderFilter.HAS)
        assertEquals(3, f.activeGroups)
        assertTrue(f.isActive)
        assertFalse(FilterState().isActive)
    }

    // ---------------- sorting ----------------
    @Test fun nameSortIsCaseInsensitiveAscending() =
        assertEquals(
            listOf("Bike insurance", "Car insurance", "Laptop warranty", "Passport", "Wi-Fi password"),
            ViewLogic.sort(all, SortBy.NAME, true).map { it.label }
        )

    @Test fun nameSortReverses() =
        assertEquals("Wi-Fi password", ViewLogic.sort(all, SortBy.NAME, false).first().label)

    @Test fun expirySortSoonestFirst() =
        assertEquals(listOf(2L, 4L, 1L, 3L), ViewLogic.sort(all, SortBy.EXPIRY, true).map { it.id }.take(4))

    @Test fun expirySortKeepsMissingDatesLastWhenAscending() =
        assertEquals(5L, ViewLogic.sort(all, SortBy.EXPIRY, true).last().id)

    @Test fun expirySortKeepsMissingDatesLastWhenDescending() =
        assertEquals(5L, ViewLogic.sort(all, SortBy.EXPIRY, false).last().id)

    @Test fun reminderSortKeepsCardsWithoutRingsLast() {
        val out = ViewLogic.sort(all, SortBy.REMINDER, true)
        assertEquals(listOf(4L, 1L), out.take(2).map { it.id })
        assertTrue(out.takeLast(3).none { it.nextRingAt != null })
    }

    @Test fun recentlyUpdatedIsNewestFirst() =
        assertEquals(5L, ViewLogic.sort(all, SortBy.UPDATED, true).first().id)

    @Test fun manualSortFollowsManualOrder() =
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), ViewLogic.sort(all, SortBy.MANUAL, true).map { it.id })

    @Test fun sortingNeverLosesOrDuplicatesRows() {
        SortBy.values().forEach { by ->
            listOf(true, false).forEach { asc ->
                val out = ViewLogic.sort(all, by, asc)
                assertEquals("$by asc=$asc", all.size, out.size)
                assertEquals("$by asc=$asc", all.map { it.id }.toSet(), out.map { it.id }.toSet())
            }
        }
    }

    // ---------------- grouping ----------------
    @Test fun categoryGroupingMakesOneSectionPerNonEmptyCategory() {
        val s = ViewLogic.group(all, all, GroupBy.CATEGORY, GroupOrder.MANUAL, categories, SortBy.EXPIRY, true, today)
        assertEquals(listOf("Insurance", "Documents", "Warranties", "Uncategorised"), s.map { it.title })
        assertEquals(2, s.first().cards.size)
    }

    @Test fun builtInCategoryIsAlwaysLast() {
        val s = ViewLogic.group(all, all, GroupBy.CATEGORY, GroupOrder.ALPHA, categories, SortBy.NAME, true, today)
        assertEquals("Uncategorised", s.last().title)
    }

    @Test fun alphaOrderSortsSectionTitles() {
        val s = ViewLogic.group(all, all, GroupBy.CATEGORY, GroupOrder.ALPHA, categories, SortBy.NAME, true, today)
        assertEquals(listOf("Documents", "Insurance", "Warranties"), s.dropLast(1).map { it.title })
    }

    @Test fun nearestDateOrderPutsSoonestExpiryCategoryFirst() {
        val s = ViewLogic.group(all, all, GroupBy.CATEGORY, GroupOrder.NEAREST_DATE, categories, SortBy.EXPIRY, true, today)
        assertEquals("Insurance", s.first().title)   // holds the overdue 2026-10-12 minus 310 days card
    }

    @Test fun sectionCountsReflectFilteringAgainstTheTotal() {
        val visible = ViewLogic.filter(all, FilterState(expiry = ExpiryFilter.OVERDUE), today)
        val s = ViewLogic.group(visible, all, GroupBy.CATEGORY, GroupOrder.MANUAL, categories, SortBy.EXPIRY, true, today)
        assertEquals(1, s.size)
        assertEquals(1, s.first().cards.size)
        assertEquals(2, s.first().totalCount)
    }

    @Test fun monthGroupingSeparatesOverdueAndUndated() {
        val s = ViewLogic.group(all, all, GroupBy.EXPIRY_MONTH, GroupOrder.MANUAL, categories, SortBy.EXPIRY, true, today)
        assertEquals("Overdue", s.first().title)
        assertEquals("No date", s.last().title)
        assertTrue(s.any { it.title == "September 2026" })
    }

    @Test fun windowGroupingBucketsByDistance() {
        val s = ViewLogic.group(all, all, GroupBy.EXPIRY_WINDOW, GroupOrder.MANUAL, categories, SortBy.EXPIRY, true, today)
        val titles = s.map { it.title }
        assertEquals(listOf("Overdue", "This month", "Next 3 months", "Later", "No date"), titles)
    }

    @Test fun reminderGroupingBucketsByRingDate() {
        val s = ViewLogic.group(all, all, GroupBy.NEXT_REMINDER, GroupOrder.MANUAL, categories, SortBy.REMINDER, true, today)
        assertEquals("This week", s.first().title)
        assertEquals("No reminder", s.last().title)
    }

    @Test fun noneGroupingReturnsASingleFlatSection() {
        val s = ViewLogic.group(all, all, GroupBy.NONE, GroupOrder.MANUAL, categories, SortBy.NAME, true, today)
        assertEquals(1, s.size)
        assertEquals(5, s.first().cards.size)
    }

    @Test fun groupingNeverLosesACard() {
        GroupBy.values().forEach { g ->
            val s = ViewLogic.group(all, all, g, GroupOrder.MANUAL, categories, SortBy.EXPIRY, true, today)
            assertEquals(g.name, all.size, s.sumOf { it.cards.size })
        }
    }

    @Test fun categorySubtitleReportsOverdueFirst() {
        val text = ViewLogic.categorySubtitle(listOf(bikeInsurance.copy(hasReminder = true), carInsurance), today)
        assertTrue(text.startsWith("1 overdue"))
    }
}
