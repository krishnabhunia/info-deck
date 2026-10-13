package com.krishna.infodeck

import com.krishna.infodeck.alarm.Snooze
import com.krishna.infodeck.core.CardRow
import com.krishna.infodeck.core.FilterState
import com.krishna.infodeck.core.GroupBy
import com.krishna.infodeck.core.GroupOrder
import com.krishna.infodeck.core.SortBy
import com.krishna.infodeck.core.ViewLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SnoozeAndTagFilterTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 8, 18)

    private fun at(h: Int, m: Int): Long =
        today.atTime(h, m).atZone(zone).toInstant().toEpochMilli()

    // ---------- B1 ----------
    @Test fun plainMinutesSnoozeForward() {
        val now = at(14, 0)
        assertEquals(now + 60 * 60_000L, Snooze.targetMillis(now, 60, zone))
    }

    @Test fun tomorrowSnoozeLandsTomorrowMorningNotInThePast() {
        val now = at(22, 30)
        val target = Snooze.targetMillis(now, Snooze.TOMORROW, zone)
        assertTrue("snooze must be in the future", target > now)
        val t = java.time.Instant.ofEpochMilli(target).atZone(zone)
        assertEquals(today.plusDays(1), t.toLocalDate())
        assertEquals(9, t.hour)
    }

    @Test fun theOldBugIsGone() {
        // -1 used to become now - 60 000 ms, i.e. a minute in the past
        val now = at(9, 0)
        assertTrue(Snooze.targetMillis(now, -1, zone) > now)
    }

    @Test fun outOfRangeSnoozeIsClamped() {
        val now = at(9, 0)
        assertEquals(now + 24 * 60 * 60_000L, Snooze.targetMillis(now, 99_999, zone))
        assertEquals(now + 60_000L, Snooze.targetMillis(now, 0, zone))
    }

    // ---------- N35 filtering and grouping ----------
    private fun row(id: Long, tags: Set<Long>, label: String = "Card $id") = CardRow(
        id = id, label = label, categoryId = 1, categoryName = "Insurance", colorIndex = 0,
        keyDate = today.plusDays(id), hasDate = true, tagIds = tags,
        tagNames = tags.map { "t$it" }, manualOrder = id.toInt(), createdAt = id, updatedAt = id
    )

    private val a = row(1, setOf(10L, 20L))
    private val b = row(2, setOf(10L))
    private val c = row(3, emptySet())
    private val all = listOf(a, b, c)

    @Test fun anyTagMatchesEitherTag() {
        val out = ViewLogic.filter(all, FilterState(tagIds = setOf(10L, 20L), tagsAll = false), today)
        assertEquals(listOf(1L, 2L), out.map { it.id })
    }

    @Test fun allTagsRequiresEveryTag() {
        val out = ViewLogic.filter(all, FilterState(tagIds = setOf(10L, 20L), tagsAll = true), today)
        assertEquals(listOf(1L), out.map { it.id })
    }

    @Test fun noTagFilterKeepsEverything() =
        assertEquals(3, ViewLogic.filter(all, FilterState(), today).size)

    @Test fun tagFilterCountsAsAnActiveGroup() =
        assertEquals(1, FilterState(tagIds = setOf(10L)).activeGroups)

    @Test fun multiTagCardAppearsUnderEachTag() {
        val sections = ViewLogic.group(
            visible = all, all = all, by = GroupBy.TAG, order = GroupOrder.MANUAL,
            categories = emptyList(), sortBy = SortBy.NAME, asc = true, today = today,
            tagNames = mapOf(10L to "renewal", 20L to "vehicle")
        )
        val renewal = sections.first { it.title == "#renewal" }
        val vehicle = sections.first { it.title == "#vehicle" }
        assertEquals(listOf(1L, 2L), renewal.cards.map { it.id })
        assertEquals(listOf(1L), vehicle.cards.map { it.id })
    }

    @Test fun untaggedSectionIsAlwaysLast() {
        val sections = ViewLogic.group(
            visible = all, all = all, by = GroupBy.TAG, order = GroupOrder.MANUAL,
            categories = emptyList(), sortBy = SortBy.NAME, asc = true, today = today,
            tagNames = mapOf(10L to "renewal", 20L to "vehicle")
        )
        assertEquals("Untagged", sections.last().title)
        assertEquals(1, sections.last().cards.size)
    }

    @Test fun groupByNoneStillReturnsOneFlatSection() {
        val sections = ViewLogic.group(
            visible = all, all = all, by = GroupBy.NONE, order = GroupOrder.MANUAL,
            categories = emptyList(), sortBy = SortBy.NAME, asc = true, today = today
        )
        assertEquals(1, sections.size)
        assertEquals(3, sections.first().cards.size)
    }

    @Test fun categoryGroupingStillGroups() {
        val sections = ViewLogic.group(
            visible = all, all = all, by = GroupBy.CATEGORY, order = GroupOrder.MANUAL,
            categories = listOf(com.krishna.infodeck.core.CategoryRow(1, "Insurance", 0, 0, false)),
            sortBy = SortBy.NAME, asc = true, today = today
        )
        assertEquals(1, sections.size)
        assertEquals("Insurance", sections.first().title)
    }
}
