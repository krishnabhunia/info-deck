package com.krishna.infodeck.core

import java.time.LocalDateTime
import java.time.ZoneId

/** Pure recovery rule: an overdue reminder needs one delivery, never a repeated one. */
object DueRules {
    fun needsDelivery(at: LocalDateTime, lastFiredAt: Long?, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        lastFiredAt == null || Fmt.fromMillis(lastFiredAt, zone).isBefore(at)
}
