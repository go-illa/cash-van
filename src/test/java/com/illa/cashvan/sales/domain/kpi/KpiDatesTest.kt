package com.illa.cashvan.sales.domain.kpi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

// Ported from the PWA's src/lib/kpiDates.test.ts, plus extra pins for the
// labels and the DST edge the PWA's algorithm produces.
class KpiDatesTest {

    private fun instant(iso: String): Instant = Instant.parse(iso)

    // monthBounds

    @Test
    fun `uses Cairo midnight with daylight-saving time in July`() {
        assertEquals(
            DateRange(start = "2026-06-30T21:00:00.000Z", end = "2026-07-31T21:00:00.000Z"),
            monthBounds(7, 2026),
        )
    }

    @Test
    fun `uses Cairo midnight standard time in January`() {
        assertEquals(
            DateRange(start = "2025-12-31T22:00:00.000Z", end = "2026-01-31T22:00:00.000Z"),
            monthBounds(1, 2026),
        )
    }

    @Test
    fun `rolls December over into the next year (extra)`() {
        assertEquals(
            DateRange(start = "2026-11-30T22:00:00.000Z", end = "2026-12-31T22:00:00.000Z"),
            monthBounds(12, 2026),
        )
    }

    // workWeekFor

    @Test
    fun `returns the Cairo Saturday-through-Thursday range`() {
        val now = instant("2026-07-15T12:00:00.000Z")
        val week = workWeekFor(now, now)

        assertEquals("2026-07-10T21:00:00.000Z", week.start)
        assertEquals("2026-07-16T21:00:00.000Z", week.end)
        assertTrue(week.isCurrent)
        assertFalse(week.isComplete)
    }

    @Test
    fun `marks the work week complete once Friday begins in Cairo`() {
        val now = instant("2026-07-17T08:00:00.000Z")
        val week = workWeekFor(now, now)

        assertTrue(week.isCurrent)
        assertTrue(week.isComplete)
    }

    @Test
    fun `labels the week Saturday to Thursday like Intl ar-EG (extra)`() {
        val now = instant("2026-07-15T12:00:00.000Z")

        assertEquals("١١ يوليو – ١٦ يوليو", workWeekFor(now, now).label)
    }

    @Test
    fun `a week before now is neither current nor incomplete (extra)`() {
        val week = workWeekFor(instant("2026-07-08T12:00:00.000Z"), instant("2026-07-15T12:00:00.000Z"))

        assertEquals("2026-07-03T21:00:00.000Z", week.start)
        assertFalse(week.isCurrent)
        assertTrue(week.isComplete)
    }

    @Test
    fun `a Saturday starts its own work week (extra)`() {
        // Saturday 2026-07-11 00:30 Cairo.
        val now = instant("2026-07-10T21:30:00.000Z")

        assertEquals("2026-07-10T21:00:00.000Z", workWeekFor(now, now).start)
    }

    @Test
    fun `ends the week before the DST-start Friday at Thursday 23h00 Cairo, as the PWA computes it (extra)`() {
        // Egypt DST starts Friday 2026-04-24 00:00 (midnight does not exist that day).
        val now = instant("2026-04-21T09:00:00.000Z")
        val week = workWeekFor(now, now)

        assertEquals("2026-04-17T22:00:00.000Z", week.start)
        assertEquals("2026-04-23T21:00:00.000Z", week.end)
        assertEquals("١٨ أبريل – ٢٣ أبريل", week.label)
    }

    // cairoCalendarDate

    @Test
    fun `keeps Cairo Saturday visible as Saturday in the calendar`() {
        assertEquals(LocalDate.of(2026, 7, 11), cairoCalendarDate(instant("2026-07-10T21:00:00.000Z")))
    }

    // KPI comparison periods

    @Test
    fun `compares the active week with the preceding week`() {
        val scope = resolveKpiComparisonScope(KpiPeriod(month = 7, year = 2026), instant("2026-07-15T12:00:00.000Z"))

        assertEquals(KpiComparisonKind.WEEK, scope.kind)
        assertEquals("الأسبوع الحالي", scope.primary.label)
        assertEquals("2026-07-03T21:00:00.000Z", scope.comparison.start)
    }

    @Test
    fun `rolls January back to the preceding December`() {
        assertEquals(KpiPeriod(month = 12, year = 2025), previousMonthPeriod(KpiPeriod(month = 1, year = 2026)))
    }

    @Test
    fun `compares a past month with the month before it (extra)`() {
        val scope = resolveKpiComparisonScope(KpiPeriod(month = 4, year = 2026), instant("2026-07-15T12:00:00.000Z"))

        assertEquals(KpiComparisonKind.MONTH, scope.kind)
        assertEquals(LabeledDateRange("2026-03-31T22:00:00.000Z", "2026-04-30T21:00:00.000Z", "إبريل 2026"), scope.primary)
        assertEquals(LabeledDateRange("2026-02-28T22:00:00.000Z", "2026-03-31T22:00:00.000Z", "مارس 2026"), scope.comparison)
    }

    // currentCairoPeriod / weekday / labels

    @Test
    fun `resolves the current period in Cairo, not UTC (extra)`() {
        assertEquals(KpiPeriod(month = 10, year = 2026), currentCairoPeriod(instant("2026-09-30T21:30:00Z")))
        assertEquals(KpiPeriod(month = 9, year = 2026), currentCairoPeriod(instant("2026-09-30T20:30:00Z")))
    }

    @Test
    fun `cairoWeekday uses JS numbering in Cairo (extra)`() {
        assertEquals(6, cairoWeekday(instant("2026-07-10T21:30:00Z"))) // Saturday 00:30 Cairo
        assertEquals(5, cairoWeekday(instant("2026-07-10T20:30:00Z"))) // Friday 23:30 Cairo
        assertEquals(0, cairoWeekday(instant("2026-07-12T12:00:00Z"))) // Sunday
    }

    @Test
    fun `arabicMonthLabel uses the PWA month names with Western digits (extra)`() {
        assertEquals("يوليو 2026", arabicMonthLabel(7, 2026))
        assertEquals("undefined 2026", arabicMonthLabel(13, 2026))
    }
}
