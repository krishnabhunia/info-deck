package com.krishna.infodeck

import com.krishna.infodeck.core.RingCalculator
import com.krishna.infodeck.data.RingDirection
import com.krishna.infodeck.data.RingUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RingCalculatorTest {

    private val base = LocalDate.of(2026, 8, 18)

    // ---------- ranges ----------
    @Test fun sameDayHasNoRange() = assertNull(RingCalculator.range(RingUnit.SAME_DAY))
    @Test fun dayRangeIs1to450() = assertEquals(1..450, RingCalculator.range(RingUnit.DAYS))
    @Test fun weekRangeIs1to60() = assertEquals(1..60, RingCalculator.range(RingUnit.WEEKS))
    @Test fun monthRangeIs1to60() = assertEquals(1..60, RingCalculator.range(RingUnit.MONTHS))
    @Test fun yearRangeIs1to20() = assertEquals(1..20, RingCalculator.range(RingUnit.YEARS))

    // ---------- clamping ----------
    @Test fun clampBelowLower() = assertEquals(1, RingCalculator.clamp(RingUnit.DAYS, 0))
    @Test fun clampNegative() = assertEquals(1, RingCalculator.clamp(RingUnit.WEEKS, -5))
    @Test fun clampAboveUpperDays() = assertEquals(450, RingCalculator.clamp(RingUnit.DAYS, 9999))
    @Test fun clampAboveUpperWeeks() = assertEquals(60, RingCalculator.clamp(RingUnit.WEEKS, 61))
    @Test fun clampAboveUpperMonths() = assertEquals(60, RingCalculator.clamp(RingUnit.MONTHS, 200))
    @Test fun clampAboveUpperYears() = assertEquals(20, RingCalculator.clamp(RingUnit.YEARS, 21))
    @Test fun clampInsideIsUntouched() = assertEquals(37, RingCalculator.clamp(RingUnit.DAYS, 37))
    @Test fun clampSameDayIsZero() = assertEquals(0, RingCalculator.clamp(RingUnit.SAME_DAY, 9))

    // ---------- same day ----------
    @Test fun sameDayIgnoresAmountAndDirection() {
        assertEquals(base, RingCalculator.ringDate(base, RingUnit.SAME_DAY, 99, RingDirection.BEFORE))
        assertEquals(base, RingCalculator.ringDate(base, RingUnit.SAME_DAY, 99, RingDirection.AFTER))
    }

    // ---------- days ----------
    @Test fun thirtyDaysBefore() =
        assertEquals(LocalDate.of(2026, 7, 19), RingCalculator.ringDate(base, RingUnit.DAYS, 30, RingDirection.BEFORE))

    @Test fun thirtyDaysAfter() =
        assertEquals(LocalDate.of(2026, 9, 17), RingCalculator.ringDate(base, RingUnit.DAYS, 30, RingDirection.AFTER))

    @Test fun maxDaysBeforeCrossesYear() =
        assertEquals(LocalDate.of(2025, 5, 25), RingCalculator.ringDate(base, RingUnit.DAYS, 450, RingDirection.BEFORE))

    @Test fun overMaxDaysIsClampedNotExtrapolated() =
        assertEquals(
            RingCalculator.ringDate(base, RingUnit.DAYS, 450, RingDirection.BEFORE),
            RingCalculator.ringDate(base, RingUnit.DAYS, 10_000, RingDirection.BEFORE)
        )

    // ---------- weeks ----------
    @Test fun sixWeeksBefore() =
        assertEquals(LocalDate.of(2026, 7, 7), RingCalculator.ringDate(base, RingUnit.WEEKS, 6, RingDirection.BEFORE))

    @Test fun sixtyWeeksAfter() =
        assertEquals(LocalDate.of(2027, 10, 12), RingCalculator.ringDate(base, RingUnit.WEEKS, 60, RingDirection.AFTER))

    // ---------- months: end-of-month clamping ----------
    @Test fun monthEndClampsToShorterMonth() {
        val mar31 = LocalDate.of(2026, 3, 31)
        assertEquals(LocalDate.of(2026, 2, 28), RingCalculator.ringDate(mar31, RingUnit.MONTHS, 1, RingDirection.BEFORE))
    }

    @Test fun monthEndClampsForwardToShorterMonth() {
        val may31 = LocalDate.of(2026, 5, 31)
        assertEquals(LocalDate.of(2026, 6, 30), RingCalculator.ringDate(may31, RingUnit.MONTHS, 1, RingDirection.AFTER))
    }

    @Test fun monthEndClampsIntoLeapFebruary() {
        val mar31 = LocalDate.of(2024, 3, 31)
        assertEquals(LocalDate.of(2024, 2, 29), RingCalculator.ringDate(mar31, RingUnit.MONTHS, 1, RingDirection.BEFORE))
    }

    @Test fun sixtyMonthsIsFiveYears() =
        assertEquals(LocalDate.of(2031, 8, 18), RingCalculator.ringDate(base, RingUnit.MONTHS, 60, RingDirection.AFTER))

    // ---------- years: leap-day clamping ----------
    @Test fun leapDayPlusOneYearClampsTo28Feb() {
        val feb29 = LocalDate.of(2024, 2, 29)
        assertEquals(LocalDate.of(2025, 2, 28), RingCalculator.ringDate(feb29, RingUnit.YEARS, 1, RingDirection.AFTER))
    }

    @Test fun leapDayPlusFourYearsStaysLeapDay() {
        val feb29 = LocalDate.of(2024, 2, 29)
        assertEquals(LocalDate.of(2028, 2, 29), RingCalculator.ringDate(feb29, RingUnit.YEARS, 4, RingDirection.AFTER))
    }

    @Test fun twentyYearsBefore() =
        assertEquals(LocalDate.of(2006, 8, 18), RingCalculator.ringDate(base, RingUnit.YEARS, 20, RingDirection.BEFORE))

    // ---------- date-time ----------
    @Test fun ringMinuteBecomesClockTime() {
        val t = RingCalculator.ringDateTime(base, RingUnit.SAME_DAY, 0, RingDirection.BEFORE, 9 * 60 + 30)
        assertEquals(9, t.hour); assertEquals(30, t.minute); assertEquals(base, t.toLocalDate())
    }

    @Test fun ringMinuteOutOfRangeIsCoerced() {
        val t = RingCalculator.ringDateTime(base, RingUnit.SAME_DAY, 0, RingDirection.BEFORE, 99_999)
        assertEquals(23, t.hour); assertEquals(59, t.minute)
    }

    @Test fun negativeRingMinuteIsCoerced() {
        val t = RingCalculator.ringDateTime(base, RingUnit.SAME_DAY, 0, RingDirection.BEFORE, -10)
        assertEquals(0, t.hour); assertEquals(0, t.minute)
    }

    // ---------- rule text ----------
    @Test fun ruleTextSameDay() =
        assertEquals("Same day · Policy Expiry", RingCalculator.ruleText(RingUnit.SAME_DAY, 0, RingDirection.BEFORE, "Policy Expiry"))

    @Test fun ruleTextSingularDay() =
        assertEquals("1 day before Expiry", RingCalculator.ruleText(RingUnit.DAYS, 1, RingDirection.BEFORE, "Expiry"))

    @Test fun ruleTextPluralDays() =
        assertEquals("30 days before Expiry", RingCalculator.ruleText(RingUnit.DAYS, 30, RingDirection.BEFORE, "Expiry"))

    @Test fun ruleTextAfterDirection() =
        assertEquals("2 months after Service", RingCalculator.ruleText(RingUnit.MONTHS, 2, RingDirection.AFTER, "Service"))

    @Test fun ruleTextClampsTheAmountItPrints() =
        assertEquals("20 years before Expiry", RingCalculator.ruleText(RingUnit.YEARS, 900, RingDirection.BEFORE, "Expiry"))

    @Test fun unitLabelsAreHumanReadable() {
        assertEquals("Same day", RingCalculator.unitLabel(RingUnit.SAME_DAY))
        assertEquals("Days", RingCalculator.unitLabel(RingUnit.DAYS))
        assertEquals("Years", RingCalculator.unitLabel(RingUnit.YEARS))
    }

    @Test fun rangeHintMentionsBounds() {
        assertTrue(RingCalculator.rangeHint(RingUnit.DAYS).contains("450"))
        assertTrue(RingCalculator.rangeHint(RingUnit.WEEKS).contains("60"))
        assertTrue(RingCalculator.rangeHint(RingUnit.YEARS).contains("20"))
    }

    // ---------- negative control: before/after are not the same date ----------
    @Test fun beforeAndAfterDifferForNonZeroAmount() {
        val b = RingCalculator.ringDate(base, RingUnit.DAYS, 5, RingDirection.BEFORE)
        val a = RingCalculator.ringDate(base, RingUnit.DAYS, 5, RingDirection.AFTER)
        assertTrue(b.isBefore(base) && a.isAfter(base))
    }
}
