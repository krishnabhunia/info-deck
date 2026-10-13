package com.krishna.infodeck.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Everything user-visible uses dd-MMM-yyyy (standing rule). */
object Fmt {
    private val DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)
    private val DATE_DOW = DateTimeFormatter.ofPattern("EEE dd-MMM-yyyy", Locale.ENGLISH)
    private val DAY = DateTimeFormatter.ofPattern("dd", Locale.ENGLISH)
    private val DOW = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
    private val MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val SHORT = DateTimeFormatter.ofPattern("dd-MMM", Locale.ENGLISH)

    fun date(d: LocalDate?): String = d?.format(DATE) ?: "-"
    fun dateDow(d: LocalDate?): String = d?.format(DATE_DOW) ?: "-"
    fun short(d: LocalDate?): String = d?.format(SHORT) ?: "-"
    fun day(d: LocalDate): String = d.format(DAY)
    fun dow(d: LocalDate): String = d.format(DOW).uppercase(Locale.ENGLISH)
    fun month(d: LocalDate): String = d.format(MONTH)

    fun time(minuteOfDay: Int): String {
        val m = minuteOfDay.coerceIn(0, 24 * 60 - 1)
        return String.format(Locale.ENGLISH, "%02d:%02d", m / 60, m % 60)
    }

    fun dateTime(t: LocalDateTime?): String =
        if (t == null) "-" else "${t.toLocalDate().format(DATE_DOW)} - ${time(t.hour * 60 + t.minute)}"

    /** "in 54 days" / "310 days ago" / "today" / "tomorrow". */
    fun relative(target: LocalDate?, today: LocalDate): String {
        if (target == null) return ""
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, target)
        return when {
            days == 0L -> "today"
            days == 1L -> "tomorrow"
            days == -1L -> "yesterday"
            days > 1 -> "in $days days"
            else -> "${-days} days ago"
        }
    }

    fun relativeShort(target: LocalDate?, today: LocalDate): String {
        if (target == null) return ""
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, target)
        return when {
            days == 0L -> "today"
            days > 0 -> "in $days d"
            else -> "${-days} d ago"
        }
    }

    fun toMillis(t: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): Long =
        t.atZone(zone).toInstant().toEpochMilli()

    fun fromMillis(ms: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
        java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDateTime()
}
