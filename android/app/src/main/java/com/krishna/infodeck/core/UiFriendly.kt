package com.krishna.infodeck.core

/**
 * Friendly text sizing (design 1.2.D1 C4). The engine from 1.2 is unchanged —
 * these are the WORDS on top of it, so nothing already tested is thrown away.
 */
enum class SizeStep(val label: String, val scale: Float, val blurb: String) {
    TINY("Tiny", 0.75f, "smaller than standard"),
    SMALL("Small", 0.85f, "a little smaller than standard"),
    STANDARD("Standard", 1.00f, "the everyday size"),
    COMFORTABLE("Comfortable", 1.15f, "a little larger than standard"),
    LARGE("Large", 1.30f, "clearly larger"),
    EXTRA_LARGE("Extra large", 1.55f, "much larger"),
    HUGE("Huge", 1.85f, "as large as it goes");

    val percent: Int get() = ScaleRules.percent(scale)
}

/** Smaller / Normal / Bigger, instead of a +/- 30 % slider. */
enum class ElementChoice(val label: String, val offset: Float) {
    SMALLER("Smaller", -0.15f), NORMAL("Normal", 0f), BIGGER("Bigger", 0.15f)
}

/** The four groups the user actually thinks in. */
enum class ElementGroup(val label: String, val roles: List<TextRole>) {
    TITLES("Card titles", listOf(TextRole.TITLE)),
    SECONDARY("Dates and second lines", listOf(TextRole.SECONDARY)),
    CHIPS("Chips and tags", listOf(TextRole.CHIP)),
    CONTROLS("Buttons and sheets", listOf(TextRole.SHEET, TextRole.SETTINGS, TextRole.HEADER))
}

object FriendlySize {

    val STEPS: List<SizeStep> = SizeStep.values().toList()

    /** Nearest named step for any scale — total, never throws. */
    fun stepFor(scale: Float): SizeStep {
        val s = ScaleRules.clampScale(scale)
        return STEPS.minByOrNull { kotlin.math.abs(it.scale - s) } ?: SizeStep.STANDARD
    }

    /** Exactly on a step? Used to decide whether to show "Comfortable" or "between sizes". */
    fun isExact(scale: Float): Boolean =
        STEPS.any { ScaleRules.percent(it.scale) == ScaleRules.percent(scale) }

    fun nameFor(scale: Float): String =
        if (isExact(scale)) stepFor(scale).label else stepFor(scale).label + " (adjusted)"

    fun blurbFor(scale: Float): String =
        "${stepFor(scale).blurb} · ${ScaleRules.percent(scale)} %"

    /** One tap bigger / smaller. Jumps to the next named step, never a raw percent. */
    fun next(scale: Float, up: Boolean): Float {
        val cur = ScaleRules.clampScale(scale)
        val ordered = STEPS.map { it.scale }
        val target = if (up) ordered.firstOrNull { it > cur + 0.001f }
        else ordered.lastOrNull { it < cur - 0.001f }
        return ScaleRules.clampScale(target ?: cur)
    }

    fun choiceFor(offset: Float): ElementChoice {
        val o = ScaleRules.clampOffset(offset)
        return ElementChoice.values().minByOrNull { kotlin.math.abs(it.offset - o) } ?: ElementChoice.NORMAL
    }

    /** Apply a group's choice to every role it covers. */
    fun applyGroup(offsets: Map<TextRole, Float>, group: ElementGroup, choice: ElementChoice): Map<TextRole, Float> {
        val out = offsets.toMutableMap()
        group.roles.forEach { out[it] = choice.offset }
        return out
    }

    fun groupChoice(offsets: Map<TextRole, Float>, group: ElementGroup): ElementChoice =
        choiceFor(offsets[group.roles.first()] ?: 0f)

    /** "Easy-read mode" = Large + Spacious + Medium, all three together. */
    val EASY_READ = UiScale(
        textScale = SizeStep.LARGE.scale,
        offsets = emptyMap(),
        spacing = RowSpacing.SPACIOUS,
        weight = TextWeightSetting.MEDIUM
    )

    fun isEasyRead(ui: UiScale): Boolean =
        ScaleRules.percent(ui.textScale) == ScaleRules.percent(EASY_READ.textScale) &&
            ui.spacing == RowSpacing.SPACIOUS && ui.weight == TextWeightSetting.MEDIUM

    /** What "Put text back to normal" restores — documented, and asserted in tests. */
    val DEFAULTS = UiScale(
        textScale = 1f, offsets = emptyMap(),
        spacing = RowSpacing.COMFORTABLE, weight = TextWeightSetting.NORMAL
    )
}

/** Countdown ring maths for the new card (design 1.2.D1 S1). */
object RingMath {
    /** 0f..1f fill. Full and red once the date has passed; empty when there is no date. */
    fun fraction(daysLeft: Long?, window: Long = 180L): Float = when {
        daysLeft == null -> 0f
        daysLeft <= 0L -> 1f
        daysLeft >= window -> 0.08f
        else -> (1f - daysLeft.toFloat() / window).coerceIn(0.08f, 1f)
    }

    enum class Band { NONE, OK, SOON, OVERDUE }

    fun band(daysLeft: Long?): Band = when {
        daysLeft == null -> Band.NONE
        daysLeft < 0L -> Band.OVERDUE
        daysLeft <= 30L -> Band.SOON
        else -> Band.OK
    }

    fun caption(daysLeft: Long?): String = when {
        daysLeft == null -> ""
        daysLeft < 0L -> "AGO"
        daysLeft == 0L -> "TODAY"
        daysLeft == 1L -> "DAY"
        else -> "DAYS"
    }

    fun number(daysLeft: Long?): String = when {
        daysLeft == null -> "-"
        daysLeft < 0L -> (-daysLeft).toString()
        daysLeft == 0L -> ""
        else -> daysLeft.toString()
    }
}
