package com.krishna.infodeck

import com.krishna.infodeck.core.DueRules
import com.krishna.infodeck.core.Fmt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class DueRulesTest {
    private val due = LocalDateTime.of(2026, 10, 13, 9, 30)
    private val zone = ZoneOffset.UTC

    @Test fun neverFiredNeedsRecovery() {
        assertTrue(DueRules.needsDelivery(due, null, zone))
    }

    @Test fun previouslyFiredMomentMustNotRepeat() {
        assertFalse(DueRules.needsDelivery(due, Fmt.toMillis(due, zone), zone))
        assertFalse(DueRules.needsDelivery(due, Fmt.toMillis(due.plusHours(1), zone), zone))
    }

    @Test fun lastFiredBeforeRescheduledDateAllowsDelivery() {
        assertTrue(DueRules.needsDelivery(due, Fmt.toMillis(due.minusDays(1), zone), zone))
    }
}
