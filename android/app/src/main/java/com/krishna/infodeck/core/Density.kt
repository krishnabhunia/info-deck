package com.krishna.infodeck.core

/** N58 — how much each Home row shows. Density decides WHAT; text size decides HOW BIG. */
enum class Density(val label: String, val rowDp: Float, val tileDp: Float, val ringDp: Float, val titleSp: Float,
                   val showMeta: Boolean, val showChips: Boolean, val ringUnit: Boolean,
                   /** B7 — the ONE gap between cards; sections use SECTION_GAP. */
                   val gapDp: Float) {
    COMPACT("Compact", 44f, 26f, 30f, 13f, false, false, false, 4f),
    COMFORTABLE("Comfortable", 60f, 34f, 38f, 14f, true, false, true, 6f),
    DETAILED("Detailed", 76f, 38f, 44f, 14f, true, true, true, 6f);

    companion object {
        const val SECTION_GAP = 8f
        val DEFAULT = COMFORTABLE
        fun of(name: String?): Density = values().firstOrNull { it.name == name } ?: run {
            if (name != null) Logger.e("Density", "unknown density '$name', using Comfortable")
            DEFAULT
        }
        /** N59 default: expanding earns its keep at Compact. */
        fun defaultExpand(d: Density): Boolean = d == COMPACT
    }
}

/** N59 — the in-place expansion is capped so the list never becomes a card view in disguise. */
object ExpandRules {
    const val MAX_VALUES = 3
    fun <T> visibleValues(all: List<T>): List<T> = all.take(MAX_VALUES)
    /** Exactly one open; opening another closes the previous; a vanished id collapses. */
    fun next(current: Long?, tapped: Long, visibleIds: Set<Long>): Long? = when {
        !visibleIds.contains(tapped) -> { Logger.e("ExpandRules", "tapped id $tapped is not visible"); null }
        current == tapped -> null
        else -> tapped
    }
    fun keep(current: Long?, visibleIds: Set<Long>): Long? = current?.takeIf { visibleIds.contains(it) }
}
