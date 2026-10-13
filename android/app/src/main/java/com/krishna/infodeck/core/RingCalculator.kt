package com.krishna.infodeck.core

import com.krishna.infodeck.data.RingDirection
import com.krishna.infodeck.data.RingUnit
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Pure ring maths. No Android, no storage — every ring moment is DERIVED here,
 * never trusted from the database (D1 C2 / D1 C3).
 */
object RingCalculator {

    /** Allowed amount range per unit. SAME_DAY takes no amount. */
    fun range(unit: RingUnit): IntRange? = when (unit) {
        RingUnit.SAME_DAY -> null
        RingUnit.DAYS -> 1..450
        RingUnit.WEEKS -> 1..60
        RingUnit.MONTHS -> 1..60
        RingUnit.YEARS -> 1..20
    }

    /** Out-of-range input clamps to the unit's bounds (D1 C2 row 7). */
    fun clamp(unit: RingUnit, amount: Int): Int {
        val r = range(unit) ?: return 0
        return when {
            amount < r.first -> r.first
            amount > r.last -> r.last
            else -> amount
        }
    }

    /** Ring date for a base date. java.time clamps month-ends and 29-Feb for us. */
    fun ringDate(base: LocalDate, unit: RingUnit, amount: Int, direction: RingDirection): LocalDate {
        if (unit == RingUnit.SAME_DAY) return base
        val n = clamp(unit, amount).toLong()
        val before = direction == RingDirection.BEFORE
        return when (unit) {
            RingUnit.DAYS -> if (before) base.minusDays(n) else base.plusDays(n)
            RingUnit.WEEKS -> if (before) base.minusWeeks(n) else base.plusWeeks(n)
            RingUnit.MONTHS -> if (before) base.minusMonths(n) else base.plusMonths(n)
            RingUnit.YEARS -> if (before) base.minusYears(n) else base.plusYears(n)
            RingUnit.SAME_DAY -> base
        }
    }

    fun ringDateTime(
        base: LocalDate, unit: RingUnit, amount: Int,
        direction: RingDirection, ringMinute: Int
    ): LocalDateTime {
        val m = ringMinute.coerceIn(0, 24 * 60 - 1)
        return LocalDateTime.of(ringDate(base, unit, amount, direction), LocalTime.of(m / 60, m % 60))
    }

    /** Human rule, e.g. "30 days before Policy Expiry" / "Same day · Policy Expiry". */
    fun ruleText(unit: RingUnit, amount: Int, direction: RingDirection, dateName: String): String {
        if (unit == RingUnit.SAME_DAY) return "Same day · $dateName"
        val n = clamp(unit, amount)
        val word = when (unit) {
            RingUnit.DAYS -> if (n == 1) "day" else "days"
            RingUnit.WEEKS -> if (n == 1) "week" else "weeks"
            RingUnit.MONTHS -> if (n == 1) "month" else "months"
            RingUnit.YEARS -> if (n == 1) "year" else "years"
            RingUnit.SAME_DAY -> ""
        }
        val dir = if (direction == RingDirection.BEFORE) "before" else "after"
        return "$n $word $dir $dateName"
    }

    fun unitLabel(unit: RingUnit): String = when (unit) {
        RingUnit.SAME_DAY -> "Same day"
        RingUnit.DAYS -> "Days"
        RingUnit.WEEKS -> "Weeks"
        RingUnit.MONTHS -> "Months"
        RingUnit.YEARS -> "Years"
    }

    fun rangeHint(unit: RingUnit): String = when (unit) {
        RingUnit.SAME_DAY -> "Rings on the date itself"
        RingUnit.DAYS -> "Amount 1 - 450 days"
        RingUnit.WEEKS -> "Amount 1 - 60 weeks"
        RingUnit.MONTHS -> "Amount 1 - 60 months"
        RingUnit.YEARS -> "Amount 1 - 20 years"
    }
}
