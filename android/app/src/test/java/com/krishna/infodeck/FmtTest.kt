package com.krishna.infodeck

import com.krishna.infodeck.core.Fmt
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FmtTest {

    private val today = LocalDate.of(2026, 8, 18)

    @Test fun datesUseDdMmmYyyy() {
        assertEquals("18-Aug-2026", Fmt.date(today))
        assertEquals("01-Jan-2027", Fmt.date(LocalDate.of(2027, 1, 1)))
    }

    @Test fun nullDateRendersAsDash() = assertEquals("-", Fmt.date(null))

    @Test fun timesArePaddedTo24Hour() {
        assertEquals("09:00", Fmt.time(9 * 60))
        assertEquals("00:05", Fmt.time(5))
        assertEquals("23:59", Fmt.time(23 * 60 + 59))
    }

    @Test fun outOfRangeMinutesAreCoerced() {
        assertEquals("23:59", Fmt.time(99999))
        assertEquals("00:00", Fmt.time(-5))
    }

    @Test fun relativeTextReadsNaturally() {
        assertEquals("today", Fmt.relative(today, today))
        assertEquals("tomorrow", Fmt.relative(today.plusDays(1), today))
        assertEquals("yesterday", Fmt.relative(today.minusDays(1), today))
        assertEquals("in 54 days", Fmt.relative(today.plusDays(54), today))
        assertEquals("310 days ago", Fmt.relative(today.minusDays(310), today))
    }

    @Test fun monthHeaderIsFullMonthAndYear() = assertEquals("August 2026", Fmt.month(today))

    @Test fun millisRoundTripKeepsTheSameLocalDate() {
        val t = today.atTime(9, 30)
        assertEquals(t, Fmt.fromMillis(Fmt.toMillis(t)))
    }
}
