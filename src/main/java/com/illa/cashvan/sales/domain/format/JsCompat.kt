package com.illa.cashvan.sales.domain.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.floor

// Helpers reproducing JavaScript's observable number/date semantics so logic
// ported from the PWA renders the same strings and makes the same decisions.

/**
 * JS `String(number)`: integers without a trailing `.0`, exponent form outside
 * [1e-6, 1e21) (`1e-7`, `1.5e+300`). Relies on `Double.toString` producing the
 * shortest round-tripping digits, which holds for every realistic value.
 */
internal fun jsNumberToString(value: Double): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
    if (value == 0.0) return "0"

    val sign = if (value < 0) "-" else ""
    val magnitude = abs(value)
    val decimal = BigDecimal(magnitude.toString()).stripTrailingZeros()
    if (magnitude >= 1e-6 && magnitude < 1e21) return sign + decimal.toPlainString()

    val digits = decimal.unscaledValue().toString()
    val exponent = digits.length - 1 - decimal.scale()
    val mantissa = if (digits.length == 1) digits else "${digits[0]}.${digits.substring(1)}"
    val exponentSign = if (exponent >= 0) "+" else "-"
    return "$sign${mantissa}e$exponentSign${abs(exponent)}"
}

/** JS `Number.prototype.toFixed`: rounds the exact binary value half-up (0.15 → "0.1", 1.25 → "1.3"). */
internal fun jsToFixed(value: Double, fractionDigits: Int): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite() || abs(value) >= 1e21) return jsNumberToString(value)
    val fixed = BigDecimal(abs(value)).setScale(fractionDigits, RoundingMode.HALF_UP).toPlainString()
    return if (value < 0) "-$fixed" else fixed
}

private fun isJsWhitespace(char: Char): Boolean = when (char) {
    '\t', '\n', '\u000B', '\u000C', '\r', ' ', '﻿', ' ', ' ' -> true
    else -> char.category == CharCategory.SPACE_SEPARATOR
}

/**
 * JS `String.prototype.trim`: strips WhiteSpace + LineTerminator only. Kotlin's
 * `trim()` also strips U+001C–U+001F and keeps U+FEFF.
 */
internal fun String.jsTrim(): String = trim(::isJsWhitespace)

/** JS `Math.round`: ties go towards +∞ (-2.5 → -2), unlike `kotlin.math.round`. */
internal fun jsMathRound(value: Double): Double {
    if (!value.isFinite()) return value
    val floored = floor(value)
    return if (value - floored >= 0.5) floored + 1 else floored
}

private val ISO_LIKE_DATE = Regex(
    "([0-9]{4})-([0-9]{2})-([0-9]{2})" +
        "(?:[T ]([0-9]{2}):([0-9]{2})(?::([0-9]{2})(?:\\.([0-9]+))?)?(Z|[+-][0-9]{2}(?::?[0-9]{2})?)?)?",
)

/**
 * `new Date(value).getTime()` for the ISO-like strings Supabase/PostgREST emit.
 * Like JS: a date-only value is UTC midnight, a date-time without an offset is
 * local time in [zone], and fractional seconds are truncated to milliseconds.
 * Returns null where JS would produce an Invalid Date.
 */
internal fun parseJsDateMillis(value: String, zone: ZoneId): Long? {
    val groups = ISO_LIKE_DATE.matchEntire(value)?.groupValues ?: return null
    return try {
        val date = LocalDate.of(groups[1].toInt(), groups[2].toInt(), groups[3].toInt())
        if (groups[4].isEmpty()) {
            date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } else {
            val millis = groups[7].take(3).padEnd(3, '0').toInt()
            val time = LocalTime.of(
                groups[4].toInt(),
                groups[5].toInt(),
                groups[6].ifEmpty { "0" }.toInt(),
                millis * 1_000_000,
            )
            val local = LocalDateTime.of(date, time)
            val offset = groups[8]
            val instant = when {
                offset.isEmpty() -> local.atZone(zone).toInstant()
                offset == "Z" -> local.toInstant(ZoneOffset.UTC)
                else -> local.toInstant(ZoneOffset.of(offset))
            }
            instant.toEpochMilli()
        }
    } catch (_: DateTimeException) {
        null
    }
}
