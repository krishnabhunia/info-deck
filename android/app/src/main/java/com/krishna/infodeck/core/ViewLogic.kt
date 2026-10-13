package com.krishna.infodeck.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

// ---------------- view options (D2 C2 / C3 / C4) ----------------

enum class GroupBy { CATEGORY, TAG, EXPIRY_MONTH, EXPIRY_WINDOW, NEXT_REMINDER, NONE;
    val label: String
        get() = when (this) {
            CATEGORY -> "Category"
            TAG -> "Tag"
            EXPIRY_MONTH -> "Expiry month"
            EXPIRY_WINDOW -> "Expiry window"
            NEXT_REMINDER -> "Next reminder"
            NONE -> "None"
        }
}

enum class GroupOrder { MANUAL, ALPHA, NEAREST_DATE;
    val label: String get() = when (this) { MANUAL -> "Manual"; ALPHA -> "A-Z"; NEAREST_DATE -> "Nearest date" }
}

enum class SortBy { NAME, EXPIRY, REMINDER, UPDATED, ADDED, MANUAL;
    val label: String
        get() = when (this) {
            NAME -> "Name"; EXPIRY -> "Expiry date"; REMINDER -> "Next reminder"
            UPDATED -> "Recently updated"; ADDED -> "Recently added"; MANUAL -> "Manual"
        }

    fun directionLabels(): Pair<String, String> = when (this) {
        NAME -> "A-Z" to "Z-A"
        UPDATED, ADDED -> "Newest first" to "Oldest first"
        MANUAL -> "My order" to "Reversed"
        else -> "Soonest first" to "Latest first"
    }
}

enum class ExpiryFilter { ANY, OVERDUE, D7, D30, D90, M12, CUSTOM;
    val label: String
        get() = when (this) {
            ANY -> "Any"; OVERDUE -> "Overdue"; D7 -> "7 days"; D30 -> "30 days"
            D90 -> "90 days"; M12 -> "12 months"; CUSTOM -> "Custom"
        }
}

enum class ReminderFilter { ANY, HAS, NONE, OVERDUE, SNOOZED, OFF;
    val label: String
        get() = when (this) {
            ANY -> "Any"; HAS -> "Has reminder"; NONE -> "No reminder"
            OVERDUE -> "Overdue"; SNOOZED -> "Snoozed"; OFF -> "Off"
        }
}

enum class ContentFilter { ANY, HAS_DATE, NO_DATE, HAS_DATA;
    val label: String
        get() = when (this) { ANY -> "Any"; HAS_DATE -> "Has date"; NO_DATE -> "No date"; HAS_DATA -> "Has data" }
}

data class FilterState(
    val categoryIds: Set<Long> = emptySet(),
    val expiry: ExpiryFilter = ExpiryFilter.ANY,
    val reminder: ReminderFilter = ReminderFilter.ANY,
    val content: ContentFilter = ContentFilter.ANY,
    val customFrom: Long? = null,
    val customTo: Long? = null,
    val tagIds: Set<Long> = emptySet(),
    /** true = card must carry ALL chosen tags, false = ANY of them. */
    val tagsAll: Boolean = false
) {
    val activeGroups: Int
        get() = (if (tagIds.isNotEmpty()) 1 else 0) +
            (if (categoryIds.isNotEmpty()) 1 else 0) +
            (if (expiry != ExpiryFilter.ANY) 1 else 0) +
            (if (reminder != ReminderFilter.ANY) 1 else 0) +
            (if (content != ContentFilter.ANY) 1 else 0)

    val isActive: Boolean get() = activeGroups > 0
}

/** One row on Home — everything the list needs, already resolved. */
data class CardRow(
    val id: Long,
    val label: String,
    val categoryId: Long,
    val categoryName: String,
    val colorIndex: Int,
    val keyDateName: String? = null,
    val keyDate: LocalDate? = null,
    val firstDataName: String? = null,
    /** B8 — the first three data variables, for the expanded row. */
    val dataPreview: List<Pair<String, String>> = emptyList(),
    /** B11 — every searchable field, and the folded index built from them. */
    val searchFields: List<SearchField> = emptyList(),
    val searchIndex: String = "",
    val firstDataValue: String? = null,
    val hasData: Boolean = false,
    val hasDate: Boolean = false,
    val nextRingAt: LocalDateTime? = null,
    val nextRingRule: String? = null,
    val tagIds: Set<Long> = emptySet(),
    val tagNames: List<String> = emptyList(),
    val fileCount: Int = 0,
    val hasReminder: Boolean = false,
    val anySnoozed: Boolean = false,
    val anyOff: Boolean = false,
    val manualOrder: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
) {
    fun isOverdue(today: LocalDate): Boolean = keyDate != null && keyDate.isBefore(today)
}

data class Section(
    val key: String,
    val title: String,
    val subtitle: String,
    val colorIndex: Int?,
    val categoryId: Long?,
    val cards: List<CardRow>,
    val totalCount: Int
)

data class CategoryRow(
    val id: Long,
    val name: String,
    val colorIndex: Int,
    val sortOrder: Int,
    val builtIn: Boolean
)

/**
 * Pure grouping / sorting / filtering. Fully unit-tested — no Android types in here.
 */
object ViewLogic {

    // ---------------- filter ----------------
    fun filter(rows: List<CardRow>, f: FilterState, today: LocalDate): List<CardRow> = rows.filter { r ->
        matchCategory(r, f) && matchTags(r, f) && matchExpiry(r, f, today) &&
            matchReminder(r, f, today) && matchContent(r, f)
    }

    private fun matchTags(r: CardRow, f: FilterState): Boolean = when {
        f.tagIds.isEmpty() -> true
        f.tagsAll -> r.tagIds.containsAll(f.tagIds)
        else -> r.tagIds.any { f.tagIds.contains(it) }
    }

    private fun matchCategory(r: CardRow, f: FilterState) =
        f.categoryIds.isEmpty() || f.categoryIds.contains(r.categoryId)

    private fun matchExpiry(r: CardRow, f: FilterState, today: LocalDate): Boolean {
        if (f.expiry == ExpiryFilter.ANY) return true
        val d = r.keyDate ?: return false
        return when (f.expiry) {
            ExpiryFilter.OVERDUE -> d.isBefore(today)
            ExpiryFilter.D7 -> withinDays(d, today, 7)
            ExpiryFilter.D30 -> withinDays(d, today, 30)
            ExpiryFilter.D90 -> withinDays(d, today, 90)
            ExpiryFilter.M12 -> withinDays(d, today, 365)
            ExpiryFilter.CUSTOM -> {
                val from = f.customFrom?.let { LocalDate.ofEpochDay(it) }
                val to = f.customTo?.let { LocalDate.ofEpochDay(it) }
                (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to))
            }
            ExpiryFilter.ANY -> true
        }
    }

    private fun withinDays(d: LocalDate, today: LocalDate, n: Long): Boolean {
        val diff = ChronoUnit.DAYS.between(today, d)
        return diff in 0..n
    }

    private fun matchReminder(r: CardRow, f: FilterState, today: LocalDate): Boolean = when (f.reminder) {
        ReminderFilter.ANY -> true
        ReminderFilter.HAS -> r.hasReminder
        ReminderFilter.NONE -> !r.hasReminder
        ReminderFilter.OVERDUE -> r.hasReminder && r.keyDate != null && r.keyDate.isBefore(today)
        ReminderFilter.SNOOZED -> r.anySnoozed
        ReminderFilter.OFF -> r.anyOff
    }

    private fun matchContent(r: CardRow, f: FilterState): Boolean = when (f.content) {
        ContentFilter.ANY -> true
        ContentFilter.HAS_DATE -> r.hasDate
        ContentFilter.NO_DATE -> !r.hasDate
        ContentFilter.HAS_DATA -> r.hasData
    }

    // ---------------- sort ----------------
    fun sort(rows: List<CardRow>, by: SortBy, asc: Boolean): List<CardRow> {
        val nameCmp = compareBy<CardRow> { it.label.lowercase() }
        val base: Comparator<CardRow> = when (by) {
            SortBy.NAME -> nameCmp.thenByDescending { it.updatedAt }
            SortBy.EXPIRY -> compareBy<CardRow> { it.keyDate?.toEpochDay() ?: Long.MAX_VALUE }.then(nameCmp)
            SortBy.REMINDER -> compareBy<CardRow> { it.nextRingAt?.toString() ?: "\uFFFF" }
                .thenBy { it.keyDate?.toEpochDay() ?: Long.MAX_VALUE }.then(nameCmp)
            SortBy.UPDATED -> compareByDescending<CardRow> { it.updatedAt }.then(nameCmp)
            SortBy.ADDED -> compareByDescending<CardRow> { it.createdAt }.then(nameCmp)
            SortBy.MANUAL -> compareBy<CardRow> { it.manualOrder }.then(nameCmp)
        }
        // Missing values always sink to the bottom, in BOTH directions (D2 C3).
        val missing: (CardRow) -> Boolean = when (by) {
            SortBy.EXPIRY -> { r -> r.keyDate == null }
            SortBy.REMINDER -> { r -> r.nextRingAt == null }
            else -> { _ -> false }
        }
        val present = rows.filterNot(missing)
        val absent = rows.filter(missing)
        val ordered = if (asc) present.sortedWith(base) else present.sortedWith(base.reversed())
        return ordered + absent.sortedWith(nameCmp)
    }

    // ---------------- group ----------------
    fun group(
        visible: List<CardRow>,
        all: List<CardRow>,
        by: GroupBy,
        order: GroupOrder,
        categories: List<CategoryRow>,
        sortBy: SortBy,
        asc: Boolean,
        today: LocalDate,
        tagNames: Map<Long, String> = emptyMap()
    ): List<Section> {
        val sorted = sort(visible, sortBy, asc)
        return when (by) {
            GroupBy.TAG -> tagSections(sorted, tagNames, order, today)
            GroupBy.NONE -> listOf(Section("all", "All cards", "${sorted.size} cards", null, null, sorted, all.size))
            GroupBy.CATEGORY -> categorySections(sorted, all, order, categories, today)
            GroupBy.EXPIRY_MONTH -> monthSections(sorted, today)
            GroupBy.EXPIRY_WINDOW -> windowSections(sorted, today)
            GroupBy.NEXT_REMINDER -> reminderSections(sorted, today)
        }
    }

    private fun categorySections(
        sorted: List<CardRow>, all: List<CardRow>, order: GroupOrder,
        categories: List<CategoryRow>, today: LocalDate
    ): List<Section> {
        val byCat = sorted.groupBy { it.categoryId }
        val totals = all.groupBy { it.categoryId }.mapValues { it.value.size }
        val cats = categories.filter { c ->
            val has = (byCat[c.id]?.isNotEmpty() == true)
            has || (c.builtIn && (totals[c.id] ?: 0) > 0)
        }
        val ordered = when (order) {
            GroupOrder.ALPHA -> cats.sortedWith(compareBy({ it.builtIn }, { it.name.lowercase() }))
            GroupOrder.NEAREST_DATE -> cats.sortedWith(
                compareBy({ it.builtIn }, { c ->
                    byCat[c.id]?.mapNotNull { it.keyDate?.toEpochDay() }?.minOrNull() ?: Long.MAX_VALUE
                })
            )
            GroupOrder.MANUAL -> cats.sortedWith(compareBy({ it.builtIn }, { it.sortOrder }, { it.name.lowercase() }))
        }
        return ordered.mapNotNull { c ->
            val cards = byCat[c.id].orEmpty()
            if (cards.isEmpty()) null
            else Section(
                key = "cat:${c.id}",
                title = c.name,
                subtitle = categorySubtitle(cards, today),
                colorIndex = c.colorIndex,
                categoryId = c.id,
                cards = cards,
                totalCount = totals[c.id] ?: cards.size
            )
        }
    }

    fun categorySubtitle(cards: List<CardRow>, today: LocalDate): String {
        val overdue = cards.count { it.isOverdue(today) && it.hasReminder }
        val nextExpiry = cards.mapNotNull { it.keyDate }.filter { !it.isBefore(today) }.minOrNull()
        val nextRing = cards.mapNotNull { it.nextRingAt }.minOrNull()
        val parts = mutableListOf<String>()
        if (overdue > 0) parts += "$overdue overdue"
        if (nextExpiry != null) parts += "Expiry ${Fmt.date(nextExpiry)}"
        if (nextRing != null) parts += "rings ${Fmt.short(nextRing.toLocalDate())}"
        if (parts.isEmpty()) parts += "${cards.size} cards"
        return parts.joinToString(" - ")
    }

    private fun tagSections(
        sorted: List<CardRow>, tagNames: Map<Long, String>, order: GroupOrder, today: LocalDate
    ): List<Section> {
        val byTag = LinkedHashMap<Long, MutableList<CardRow>>()
        val untagged = mutableListOf<CardRow>()
        sorted.forEach { r ->
            if (r.tagIds.isEmpty()) untagged.add(r)
            else r.tagIds.forEach { id -> byTag.getOrPut(id) { mutableListOf() }.add(r) }
        }
        val keys = when (order) {
            GroupOrder.ALPHA -> byTag.keys.sortedBy { tagNames[it]?.lowercase() ?: "" }
            GroupOrder.NEAREST_DATE -> byTag.keys.sortedBy { id ->
                byTag[id]?.mapNotNull { it.keyDate?.toEpochDay() }?.minOrNull() ?: Long.MAX_VALUE
            }
            GroupOrder.MANUAL -> byTag.keys.sortedByDescending { byTag[it]?.size ?: 0 }
        }
        val out = keys.mapNotNull { id ->
            val cards = byTag[id].orEmpty()
            if (cards.isEmpty()) null else Section(
                key = "tag:" + id,
                title = "#" + (tagNames[id] ?: "tag"),
                subtitle = categorySubtitle(cards, today),
                colorIndex = null,
                categoryId = null,
                cards = cards,
                totalCount = cards.size
            )
        }.toMutableList()
        if (untagged.isNotEmpty()) out += Section(
            "tag:none", "Untagged", untagged.size.toString() + " cards", null, null, untagged, untagged.size
        )
        return out
    }

    private fun monthSections(sorted: List<CardRow>, today: LocalDate): List<Section> {
        val out = mutableListOf<Section>()
        val overdue = sorted.filter { it.keyDate != null && it.keyDate.isBefore(today) }
        if (overdue.isNotEmpty())
            out += Section("m:overdue", "Overdue", "Key date already passed", null, null, overdue, overdue.size)
        val dated = sorted.filter { it.keyDate != null && !it.keyDate.isBefore(today) }
        dated.groupBy { it.keyDate!!.withDayOfMonth(1) }
            .toSortedMap()
            .forEach { (month, cards) ->
                val nearest = cards.mapNotNull { it.keyDate }.minOrNull()
                out += Section(
                    "m:${month}", Fmt.month(month),
                    "${cards.size} cards - nearest ${Fmt.short(nearest)}", null, null, cards, cards.size
                )
            }
        val none = sorted.filter { it.keyDate == null }
        if (none.isNotEmpty())
            out += Section("m:none", "No date", "Cards without any date variable", null, null, none, none.size)
        return out
    }

    private fun windowSections(sorted: List<CardRow>, today: LocalDate): List<Section> {
        val buckets = linkedMapOf(
            "Overdue" to mutableListOf<CardRow>(),
            "This week" to mutableListOf(),
            "This month" to mutableListOf(),
            "Next 3 months" to mutableListOf(),
            "Later" to mutableListOf(),
            "No date" to mutableListOf()
        )
        sorted.forEach { r ->
            val d = r.keyDate
            val key = when {
                d == null -> "No date"
                d.isBefore(today) -> "Overdue"
                ChronoUnit.DAYS.between(today, d) <= 7 -> "This week"
                ChronoUnit.DAYS.between(today, d) <= 31 -> "This month"
                ChronoUnit.DAYS.between(today, d) <= 92 -> "Next 3 months"
                else -> "Later"
            }
            buckets.getValue(key).add(r)
        }
        return buckets.filterValues { it.isNotEmpty() }.map { (k, v) ->
            Section("w:$k", k, "${v.size} cards", null, null, v, v.size)
        }
    }

    private fun reminderSections(sorted: List<CardRow>, today: LocalDate): List<Section> {
        val buckets = linkedMapOf(
            "Overdue" to mutableListOf<CardRow>(),
            "Today" to mutableListOf(),
            "This week" to mutableListOf(),
            "This month" to mutableListOf(),
            "Later" to mutableListOf(),
            "No reminder" to mutableListOf()
        )
        sorted.forEach { r ->
            val t = r.nextRingAt
            val key = when {
                t == null -> "No reminder"
                t.toLocalDate().isBefore(today) -> "Overdue"
                t.toLocalDate() == today -> "Today"
                ChronoUnit.DAYS.between(today, t.toLocalDate()) <= 7 -> "This week"
                ChronoUnit.DAYS.between(today, t.toLocalDate()) <= 31 -> "This month"
                else -> "Later"
            }
            buckets.getValue(key).add(r)
        }
        return buckets.filterValues { it.isNotEmpty() }.map { (k, v) ->
            Section("r:$k", k, "${v.size} cards", null, null, v, v.size)
        }
    }
}
