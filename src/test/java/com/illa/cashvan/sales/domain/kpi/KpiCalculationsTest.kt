package com.illa.cashvan.sales.domain.kpi

import com.illa.cashvan.sales.domain.model.RouteMerchant
import com.illa.cashvan.sales.domain.model.VISITS_PER_MERCHANT_PER_MONTH
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil

// Ported from the PWA's src/utils/__tests__/kpiCalculations.test.ts. Reference
// dates are the calendar days the TS `new Date('2026-04-xx')` has in Cairo, and
// "now" is pinned (the TS reads the real clock).
class KpiCalculationsTest {

    private val cairo: ZoneId = ZoneId.of("Africa/Cairo")

    /** 2026-04-15 (30 days → 26 working days). */
    private val now: Instant = Instant.parse("2026-04-15T10:00:00Z")
    private val daysInMonth = 30

    private fun merchant(id: Int) = RouteMerchant(
        merchantId = "merchant-$id",
        merchantCode = "TEST",
        merchantName = "Test Merchant",
        signName = null,
        latitude = null,
        longitude = null,
        location = null,
        priceTier = null,
        phoneNumber = null,
        secondaryPhone = null,
        detailedAddress = null,
        governorate = null,
        frontdoorCode = null,
        googleLink = null,
        workingDays = emptyList(),
        regionId = null,
        fawryDeviceNumber = null,
        wantsFawryDevice = null,
        routeId = "route-1",
        routeName = "Test Route",
        visitCount = 0,
        lastVisitDate = null,
        hasOrder = false,
        targetCompleted = false,
    )

    private fun merchants(count: Int) = List(count, ::merchant)

    private fun visit(merchantId: String, date: String, type: String, isAdHoc: Boolean? = null) =
        RawVisit(merchantId, date, type, isAdHoc)

    private fun weekBased(visits: List<RawVisit>, reference: LocalDate) =
        computeWeekBasedVisitData(visits, reference, cairo)

    private fun kpis(merchantCount: Int, todayVisits: Int, visitsData: VisitsData) =
        calculateKPIs(merchants(merchantCount), todayVisits, visitsData, now = now, zone = cairo)

    private fun april(day: Int) = LocalDate.of(2026, 4, day)

    @Test
    fun `VISITS_PER_MERCHANT_PER_MONTH should be set to 2`() {
        assertEquals(2, VISITS_PER_MERCHANT_PER_MONTH)
    }

    // computeMonthlyVisitData

    @Test
    fun `caps each merchant at the two required monthly planned visits`() {
        val result = computeMonthlyVisitData(
            listOf(
                visit("A", "2026-04-02T10:00:00", "with_order"),
                visit("A", "2026-04-03T10:00:00", "without_order"),
                visit("A", "2026-04-05T10:00:00", "with_order"),
                visit("B", "2026-04-09T10:00:00", "without_order"),
            ),
        )

        assertEquals(3, result.totalPlanned)
        assertEquals(2, result.totalOrders)
    }

    @Test
    fun `excludes ad-hoc visits from planned monthly completion`() {
        val result = computeMonthlyVisitData(
            listOf(
                visit("A", "2026-04-02T10:00:00", "with_order", isAdHoc = true),
                visit("A", "2026-04-03T10:00:00", "without_order"),
            ),
        )

        assertEquals(1, result.totalPlanned)
        assertEquals(0, result.totalOrders)
    }

    // getWeekIndex

    @Test
    fun `getWeekIndex should return 0 for days 1-7`() {
        assertEquals(0, getWeekIndex(1))
        assertEquals(0, getWeekIndex(7))
    }

    @Test
    fun `getWeekIndex should return 1 for days 8-14`() {
        assertEquals(1, getWeekIndex(8))
        assertEquals(1, getWeekIndex(14))
    }

    @Test
    fun `getWeekIndex should return 2 for days 15-21`() {
        assertEquals(2, getWeekIndex(15))
        assertEquals(2, getWeekIndex(21))
    }

    @Test
    fun `getWeekIndex should return 3 for days 22-31`() {
        assertEquals(3, getWeekIndex(22))
        assertEquals(3, getWeekIndex(28))
        assertEquals(3, getWeekIndex(31))
    }

    // getWeeksElapsed

    @Test
    fun `getWeeksElapsed should return 1 for days 1-7`() {
        assertEquals(1, getWeeksElapsed(april(1)))
        assertEquals(1, getWeeksElapsed(april(7)))
    }

    @Test
    fun `getWeeksElapsed should return 2 for days 8-14`() {
        assertEquals(2, getWeeksElapsed(april(8)))
        assertEquals(2, getWeeksElapsed(april(14)))
    }

    @Test
    fun `getWeeksElapsed should cap at 2 after the second week`() {
        assertEquals(2, getWeeksElapsed(april(15)))
        assertEquals(2, getWeeksElapsed(april(21)))
        assertEquals(2, getWeeksElapsed(april(22)))
        assertEquals(2, getWeeksElapsed(april(30)))
    }

    @Test
    fun `getWeeksElapsed should cap at VISITS_PER_MERCHANT_PER_MONTH`() {
        assertEquals(VISITS_PER_MERCHANT_PER_MONTH, getWeeksElapsed(april(30)))
    }

    // computeWeekBasedVisitData

    @Test
    fun `week-based should return 0 for empty visits`() {
        val result = weekBased(emptyList(), april(10))

        assertEquals(0, result.totalPlanned)
        assertEquals(0, result.totalOrders)
        assertEquals(2, result.weeksElapsed)
    }

    @Test
    fun `week-based should count each merchant-week once (dedup same-week visits)`() {
        val result = weekBased(
            listOf(
                visit("A", "2026-04-02T10:00:00", "with_order"),
                visit("A", "2026-04-03T10:00:00", "without_order"),
                visit("A", "2026-04-05T10:00:00", "with_order"),
            ),
            april(6),
        )

        assertEquals(1, result.totalPlanned)
        assertEquals(1, result.totalOrders)
    }

    @Test
    fun `week-based should track visits across multiple weeks`() {
        val result = weekBased(
            listOf(
                visit("A", "2026-04-03T10:00:00", "with_order"),
                visit("A", "2026-04-10T10:00:00", "with_order"),
                visit("A", "2026-04-18T10:00:00", "without_order"),
            ),
            april(25),
        )

        assertEquals(3, result.totalPlanned)
        assertEquals(2, result.totalOrders)
        assertEquals(2, result.weeksElapsed)
    }

    @Test
    fun `week-based should track totalOrders per week independently`() {
        val result = weekBased(
            listOf(
                visit("A", "2026-04-02T10:00:00", "with_order"),
                visit("A", "2026-04-09T10:00:00", "without_order"),
            ),
            april(15),
        )

        assertEquals(2, result.totalPlanned)
        assertEquals(1, result.totalOrders)
    }

    @Test
    fun `week-based should handle multiple merchants correctly`() {
        val result = weekBased(
            listOf(
                visit("A", "2026-04-02T10:00:00", "with_order"),
                visit("B", "2026-04-03T10:00:00", "with_order"),
                visit("C", "2026-04-05T10:00:00", "without_order"),
            ),
            april(5),
        )

        assertEquals(3, result.totalPlanned)
        assertEquals(2, result.totalOrders)
        assertEquals(1, result.weeksElapsed)
    }

    @Test
    fun `week-based should simulate 150 merchants full month scenario`() {
        val visits = (0 until 150).flatMap { index ->
            listOf("02", "09", "16", "23").map { day -> visit("merchant-$index", "2026-04-${day}T10:00:00", "with_order") }
        }

        val result = weekBased(visits, april(25))

        assertEquals(600, result.totalPlanned)
        assertEquals(600, result.totalOrders)
        assertEquals(2, result.weeksElapsed)
    }

    @Test
    fun `week-based should handle partial month correctly`() {
        val visits = (0 until 150).map { visit("m-$it", "2026-04-02T10:00:00", "with_order") }

        val result = weekBased(visits, april(10))

        assertEquals(150, result.totalPlanned)
        assertEquals(2, result.weeksElapsed)
    }

    @Test
    fun `week-based reads the visit day in the given zone (extra)`() {
        // 22:30 UTC on Apr 7 is already Apr 8 in Cairo → week 1, not week 0.
        val result = weekBased(
            listOf(
                visit("A", "2026-04-02T10:00:00+00:00", "with_order"),
                visit("A", "2026-04-07T22:30:00+00:00", "without_order"),
            ),
            april(10),
        )

        assertEquals(2, result.totalPlanned)
        assertEquals(1, result.totalOrders)
    }

    @Test
    fun `week-based counts an invalid date as its own week, like the PWA's NaN week (extra)`() {
        val result = weekBased(
            listOf(
                visit("A", "2026-04-02T10:00:00", "with_order"),
                visit("A", "not-a-date", "with_order"),
                visit("A", "also-not-a-date", "without_order"),
            ),
            april(10),
        )

        assertEquals(2, result.totalPlanned)
        assertEquals(2, result.totalOrders)
    }

    // getEmptyKPIs

    @Test
    fun `getEmptyKPIs should return all zeros except dailyVisitTarget`() {
        val kpis = getEmptyKPIs()

        assertEquals(0, kpis.totalMerchants)
        assertEquals(0, kpis.totalPlannedVisits)
        assertEquals(0, kpis.plannedVisitsCompleted)
        assertEquals(0, kpis.visitsWithOrder)
        assertEquals(0, kpis.totalVisits)
        assertEquals(0, kpis.expectedVisits)
        assertEquals(0.0, kpis.strikeRate, 0.0)
        assertEquals(0.0, kpis.completionRate, 0.0)
        assertEquals(0, kpis.todayVisits)
        assertEquals(0, kpis.dailyVisitTarget)
    }

    // calculateKPIs

    @Test
    fun `Case 1 Standard Execution - all planned visits done, half with orders`() {
        val result = kpis(5, 5, MonthlyVisitData(totalPlanned = 10, totalOrders = 5))

        assertEquals(5, result.totalMerchants)
        assertEquals(10, result.totalPlannedVisits)
        assertEquals(10, result.plannedVisitsCompleted)
        assertEquals(5, result.visitsWithOrder)
        assertEquals(100.0, result.completionRate, 0.0)
        assertEquals(50.0, result.strikeRate, 0.0)
    }

    @Test
    fun `Case 2 Ad-hoc visits excluded - only planned visits count`() {
        val result = kpis(30, 3, MonthlyVisitData(totalPlanned = 10, totalOrders = 9))

        assertEquals(30, result.totalMerchants)
        assertEquals(60, result.totalPlannedVisits)
        assertEquals(10, result.plannedVisitsCompleted)
        assertEquals(9, result.visitsWithOrder)
        assertEquals(16.7, result.completionRate, 0.0)
        assertEquals(90.0, result.strikeRate, 0.0)
    }

    @Test
    fun `Case 3 Phone orders excluded - no orders from physical visits`() {
        val result = kpis(13, 8, MonthlyVisitData(totalPlanned = 26, totalOrders = 0))

        assertEquals(13, result.totalMerchants)
        assertEquals(26, result.totalPlannedVisits)
        assertEquals(26, result.plannedVisitsCompleted)
        assertEquals(0, result.visitsWithOrder)
        assertEquals(100.0, result.completionRate, 0.0)
        assertEquals(0.0, result.strikeRate, 0.0)
    }

    @Test
    fun `Case 4 Multiple orders from single visit counts as 1 successful visit`() {
        val result = kpis(5, 20, MonthlyVisitData(totalPlanned = 10, totalOrders = 1))

        assertEquals(5, result.totalMerchants)
        assertEquals(10, result.totalPlannedVisits)
        assertEquals(10, result.plannedVisitsCompleted)
        assertEquals(1, result.visitsWithOrder)
        assertEquals(100.0, result.completionRate, 0.0)
        assertEquals(10.0, result.strikeRate, 0.0)
    }

    @Test
    fun `should return 0 percent for both rates when there are zero merchants`() {
        val result = kpis(0, 0, WeekBasedVisitData(totalPlanned = 0, totalOrders = 0, weeksElapsed = 4))

        assertEquals(0.0, result.completionRate, 0.0)
        assertEquals(0.0, result.strikeRate, 0.0)
        assertEquals(0, result.totalPlannedVisits)
    }

    @Test
    fun `should return 100 percent strike rate when all visits have orders`() {
        val result = kpis(3, 5, MonthlyVisitData(totalPlanned = 6, totalOrders = 6))

        assertEquals(100.0, result.strikeRate, 0.0)
    }

    @Test
    fun `should return 100 percent completion rate when all planned visits are done`() {
        val result = kpis(3, 12, MonthlyVisitData(totalPlanned = 6, totalOrders = 0))

        assertEquals(100.0, result.completionRate, 0.0)
        assertEquals(0.0, result.strikeRate, 0.0)
    }

    @Test
    fun `should handle partial completion accurately`() {
        val result = kpis(10, 7, MonthlyVisitData(totalPlanned = 15, totalOrders = 9))

        assertEquals(75.0, result.completionRate, 0.0)
        assertEquals(60.0, result.strikeRate, 0.0)
    }

    @Test
    fun `should return 0 percent strike rate when visits are done but none have orders`() {
        val result = kpis(5, 5, MonthlyVisitData(totalPlanned = 7, totalOrders = 0))

        assertEquals(70.0, result.completionRate, 0.0)
        assertEquals(0.0, result.strikeRate, 0.0)
    }

    @Test
    fun `adds ad-hoc orders to the strike-rate numerator (extra)`() {
        val result = calculateKPIs(
            merchants(10),
            todayVisitCount = 0,
            visitsData = MonthlyVisitData(totalPlanned = 10, totalOrders = 4),
            adhocVisitOrderCount = 3,
            now = now,
            zone = cairo,
        )

        assertEquals(7, result.visitsWithOrder)
        assertEquals(70.0, result.strikeRate, 0.0)
    }

    // monthly target denominator

    @Test
    fun `should ignore weeksElapsed and use the two-visit monthly denominator`() {
        val result = kpis(10, 10, WeekBasedVisitData(totalPlanned = 10, totalOrders = 5, weeksElapsed = 1))

        assertEquals(20, result.totalPlannedVisits)
        assertEquals(50.0, result.completionRate, 0.0)
    }

    @Test
    fun `should show partial completion with weeksElapsed=2`() {
        val result = kpis(10, 5, WeekBasedVisitData(totalPlanned = 15, totalOrders = 8, weeksElapsed = 2))

        assertEquals(20, result.totalPlannedVisits)
        assertEquals(75.0, result.completionRate, 0.0)
    }

    @Test
    fun `should keep dailyVisitTarget based on full month`() {
        val result = kpis(150, 10, WeekBasedVisitData(totalPlanned = 150, totalOrders = 75, weeksElapsed = 1))

        val expected = ceil(150.0 * VISITS_PER_MERCHANT_PER_MONTH / (daysInMonth - VACATION_DAYS_PER_MONTH)).toInt()
        assertEquals(expected, result.dailyVisitTarget)
        assertEquals(12, result.dailyVisitTarget) // 300 / 26 = 11.5 → 12
    }

    // dynamic dailyVisitTarget

    @Test
    fun `dailyVisitTarget should be 0 when there are no merchants`() {
        val result = kpis(0, 0, WeekBasedVisitData(totalPlanned = 0, totalOrders = 0, weeksElapsed = 4))

        assertEquals(0, result.dailyVisitTarget)
    }

    @Test
    fun `should calculate dynamic target based on merchants and working days`() {
        val result = kpis(150, 10, WeekBasedVisitData(totalPlanned = 600, totalOrders = 300, weeksElapsed = 4))

        val expected = ceil(150.0 * VISITS_PER_MERCHANT_PER_MONTH / (daysInMonth - VACATION_DAYS_PER_MONTH)).toInt()
        assertEquals(expected, result.dailyVisitTarget)
        assertTrue(result.dailyVisitTarget > 0)
    }

    @Test
    fun `should round up (ceiling) for partial results`() {
        val result = kpis(5, 0, WeekBasedVisitData(totalPlanned = 20, totalOrders = 10, weeksElapsed = 4))

        // As in the TS test: ceil(20 / working days); both it and ceil(10 / working days) are 1.
        val expected = ceil(20.0 / (daysInMonth - VACATION_DAYS_PER_MONTH)).toInt()
        assertEquals(expected, result.dailyVisitTarget)
        assertTrue(result.dailyVisitTarget >= 1)
    }

    @Test
    fun `should use VACATION_DAYS_PER_MONTH constant`() {
        assertEquals(4, VACATION_DAYS_PER_MONTH)
    }

    @Test
    fun `daily target uses the month of now in the given zone (extra)`() {
        // 2026-02-28 23:30 UTC is already March 1 in Cairo: 31 days → 27 working days.
        val result = calculateKPIs(
            merchants(150),
            todayVisitCount = 0,
            visitsData = MonthlyVisitData(0, 0),
            now = Instant.parse("2026-02-28T23:30:00Z"),
            zone = cairo,
        )

        assertEquals(12, result.dailyVisitTarget) // ceil(300 / 27) = 12 (February would give ceil(300 / 24) = 13)
    }

    // completion rate cap

    @Test
    fun `should cap completion rate at 100 percent`() {
        val result = kpis(3, 15, WeekBasedVisitData(totalPlanned = 15, totalOrders = 10, weeksElapsed = 4))

        assertEquals(100.0, result.completionRate, 0.0)
    }
}
