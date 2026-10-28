package com.illa.cashvan.sales.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Start of the current month on the device clock, as the PWA computes it, in UTC ISO form. */
fun startOfMonthIso(now: ZonedDateTime = ZonedDateTime.now()): String =
    now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant().toString()

fun startOfTodayIso(now: ZonedDateTime = ZonedDateTime.now()): String =
    now.truncatedTo(ChronoUnit.DAYS).toInstant().toString()

/** Parses Postgres timestamps (`2026-10-27T10:15:30.123+00:00`, `...Z`, or without an offset as UTC). */
fun parseTimestamp(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(value).atZone(ZoneId.of("UTC")).toInstant() }.getOrNull()
}
