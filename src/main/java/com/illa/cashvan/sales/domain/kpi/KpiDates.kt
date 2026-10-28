package com.illa.cashvan.sales.domain.kpi

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Cairo-timezone date helpers for the KPI pages, ported from the PWA's
// src/lib/kpiDates.ts. All KPI logic runs in Africa/Cairo regardless of the
// device zone. Months are 1-based to match the get_kpi_* / agent-kpi-mobile
// conventions. Range bounds are JS `toISOString()` strings for Supabase
// gte/lt filters.

val CAIRO_ZONE: ZoneId = ZoneId.of("Africa/Cairo")

/** The PWA's month names (note "إبريل", unlike Intl's "أبريل"). */
val ARABIC_MONTHS: List<String> = listOf(
    "يناير", "فبراير", "مارس", "إبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
)

// Month names as rendered by Intl 'ar-EG' { month: 'short' } (CLDR "ar" abbreviated = wide).
private val INTL_AR_SHORT_MONTHS = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
)

private const val CURRENT_WEEK_LABEL = "الأسبوع الحالي"
private const val PREVIOUS_WEEK_LABEL = "الأسبوع السابق"

private val JS_ISO_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

/** JS `Date.prototype.toISOString()`: always UTC with milliseconds. */
internal fun jsIsoString(instant: Instant): String = JS_ISO_FORMATTER.format(instant)

/** 1-based month and year. */
data class KpiPeriod(val month: Int, val year: Int)

/** `[start, end)` as ISO strings. */
data class DateRange(val start: String, val end: String)

data class LabeledDateRange(val start: String, val end: String, val label: String)

/** Saturday-start work week; [end] is Friday 00:00 Cairo and exclusive. */
data class WorkWeek(
    val start: String,
    val end: String,
    val startDate: Instant,
    val endDate: Instant,
    val isCurrent: Boolean,
    val isComplete: Boolean,
    /** e.g. "١١ يوليو – ١٦ يوليو" (Saturday – Thursday, Arabic-Indic digits). */
    val label: String,
)

enum class KpiComparisonKind { WEEK, MONTH }

data class KpiComparisonScope(
    val kind: KpiComparisonKind,
    val primary: LabeledDateRange,
    val comparison: LabeledDateRange,
)

private fun cairoDate(instant: Instant): LocalDate = instant.atZone(CAIRO_ZONE).toLocalDate()

/**
 * The Cairo calendar day of [date]. The PWA returns a device-local Date at noon
 * of that day for its date pickers; a [LocalDate] carries the same information.
 */
fun cairoCalendarDate(date: Instant): LocalDate = cairoDate(date)

/**
 * Cairo midnight of [date], computed like the PWA: Cairo's offset is sampled at
 * UTC midnight of that day. This only differs from `atStartOfDay(CAIRO_ZONE)` on
 * the DST-start Friday (local midnight does not exist), where it yields
 * Thursday 23:00 Cairo instead of Friday 01:00.
 */
private fun cairoMidnightAsUtc(date: LocalDate): Instant {
    val wallClockUtc = date.atStartOfDay(ZoneOffset.UTC).toInstant()
    val offsetSeconds = CAIRO_ZONE.rules.getOffset(wallClockUtc).totalSeconds
    return wallClockUtc.minusSeconds(offsetSeconds.toLong())
}

private fun addCairoDays(date: Instant, amount: Long): Instant = cairoMidnightAsUtc(cairoDate(date).plusDays(amount))

/** Current Cairo month (1–12) and year. */
fun currentCairoPeriod(referenceDate: Instant = Instant.now()): KpiPeriod {
    val date = cairoDate(referenceDate)
    return KpiPeriod(month = date.monthValue, year = date.year)
}

/**
 * `[start, end)` of the given Cairo month: its first day 00:00 Cairo and the
 * first day of the next month, as UTC ISO strings. Out-of-range months roll
 * over like JS `Date.UTC`.
 */
fun monthBounds(month: Int, year: Int): DateRange {
    val firstOfYear = LocalDate.of(year, 1, 1)
    val start = cairoMidnightAsUtc(firstOfYear.plusMonths(month - 1L))
    val end = cairoMidnightAsUtc(firstOfYear.plusMonths(month.toLong()))
    return DateRange(jsIsoString(start), jsIsoString(end))
}

/** e.g. "يوليو 2026" (Western digits). A month outside 1–12 renders as "undefined", like JS. */
fun arabicMonthLabel(month: Int, year: Int): String =
    "${ARABIC_MONTHS.getOrNull(month - 1) ?: "undefined"} $year"

/** JS day-of-week (0 = Sunday … 6 = Saturday) of [date] in Cairo. */
fun cairoWeekday(date: Instant): Int = date.atZone(CAIRO_ZONE).dayOfWeek.value % 7

private fun daysSinceSaturday(date: Instant): Long {
    val weekday = cairoWeekday(date)
    return if (weekday == 6) 0 else weekday + 1L
}

private fun toArabicIndicDigits(text: String): String =
    text.map { if (it in '0'..'9') '٠' + (it - '0') else it }.joinToString("")

/** Intl 'ar-EG' { day: 'numeric', month: 'short', timeZone: 'Africa/Cairo' }, e.g. "١١ يوليو". */
private fun formatArabicDayMonth(date: Instant): String {
    val day = cairoDate(date)
    return "${toArabicIndicDigits(day.dayOfMonth.toString())} ${INTL_AR_SHORT_MONTHS[day.monthValue - 1]}"
}

/** Saturday-start work week containing [referenceDate]. */
fun workWeekFor(referenceDate: Instant, now: Instant = Instant.now()): WorkWeek {
    val startDate = addCairoDays(referenceDate, -daysSinceSaturday(referenceDate))
    val endDate = addCairoDays(startDate, 6)
    val currentStart = addCairoDays(now, -daysSinceSaturday(now))
    val lastWorkDay = addCairoDays(startDate, 5)

    return WorkWeek(
        start = jsIsoString(startDate),
        end = jsIsoString(endDate),
        startDate = startDate,
        endDate = endDate,
        isCurrent = startDate == currentStart,
        isComplete = endDate <= now,
        label = "${formatArabicDayMonth(startDate)} – ${formatArabicDayMonth(lastWorkDay)}",
    )
}

fun previousMonthPeriod(period: KpiPeriod): KpiPeriod =
    if (period.month == 1) KpiPeriod(month = 12, year = period.year - 1) else period.copy(month = period.month - 1)

/**
 * The current Cairo month compares this work week with the previous one; any
 * other month compares with the month before it.
 */
fun resolveKpiComparisonScope(period: KpiPeriod, now: Instant = Instant.now()): KpiComparisonScope {
    if (period == currentCairoPeriod(now)) {
        val primary = workWeekFor(now, now)
        val comparison = workWeekFor(addCairoDays(primary.startDate, -1), now)
        return KpiComparisonScope(
            kind = KpiComparisonKind.WEEK,
            primary = LabeledDateRange(primary.start, primary.end, CURRENT_WEEK_LABEL),
            comparison = LabeledDateRange(comparison.start, comparison.end, PREVIOUS_WEEK_LABEL),
        )
    }

    val previous = previousMonthPeriod(period)
    val primaryBounds = monthBounds(period.month, period.year)
    val comparisonBounds = monthBounds(previous.month, previous.year)
    return KpiComparisonScope(
        kind = KpiComparisonKind.MONTH,
        primary = LabeledDateRange(primaryBounds.start, primaryBounds.end, arabicMonthLabel(period.month, period.year)),
        comparison = LabeledDateRange(
            comparisonBounds.start,
            comparisonBounds.end,
            arabicMonthLabel(previous.month, previous.year),
        ),
    )
}
