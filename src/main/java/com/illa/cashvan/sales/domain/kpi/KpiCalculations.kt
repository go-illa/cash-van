package com.illa.cashvan.sales.domain.kpi

import com.illa.cashvan.sales.domain.format.jsMathRound
import com.illa.cashvan.sales.domain.format.parseJsDateMillis
import com.illa.cashvan.sales.domain.model.RouteMerchant
import com.illa.cashvan.sales.domain.model.VISITS_PER_MERCHANT_PER_MONTH
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.ceil

// Route KPI math ported from the PWA's src/utils/kpiCalculations.ts.

/**
 * Vacation/rest days deducted per month when calculating the daily visit target.
 * Working days = days in the current month - [VACATION_DAYS_PER_MONTH].
 */
const val VACATION_DAYS_PER_MONTH = 4

/** Week index (0–3) of a day of month: days 1–7 → 0, 8–14 → 1, 15–21 → 2, 22–31 → 3. */
fun getWeekIndex(dayOfMonth: Int): Int = minOf(Math.floorDiv(dayOfMonth - 1, 7), 3)

/**
 * Weeks started in the month as of [referenceDate] (the calendar day of the
 * PWA's `Date` in the device zone), capped at [VISITS_PER_MERCHANT_PER_MONTH].
 * Legacy helper kept for parity.
 */
fun getWeeksElapsed(referenceDate: LocalDate): Int =
    minOf(Math.floorDiv(referenceDate.dayOfMonth - 1, 7) + 1, VISITS_PER_MERCHANT_PER_MONTH)

/** Visit record shape needed for monthly/week-based aggregation. */
data class RawVisit(
    val merchantId: String,
    /** ISO timestamp, parsed like JS `new Date(value)`. */
    val visitDate: String,
    val visitType: String,
    val isAdHoc: Boolean? = null,
)

/** Aggregated planned visits and orders consumed by [calculateKPIs]. */
interface VisitsData {
    val totalPlanned: Int
    val totalOrders: Int
}

data class MonthlyVisitData(
    override val totalPlanned: Int,
    override val totalOrders: Int,
) : VisitsData

data class WeekBasedVisitData(
    override val totalPlanned: Int,
    override val totalOrders: Int,
    /** Legacy field: the monthly target no longer uses elapsed weeks. */
    val weeksElapsed: Int,
) : VisitsData

private const val WITH_ORDER = "with_order"

private class MerchantVisitTally {
    var visits = 0
    var visitsWithOrder = 0
}

/**
 * Monthly visit data: each merchant contributes at most
 * [VISITS_PER_MERCHANT_PER_MONTH] planned visits; ad-hoc visits are excluded.
 */
fun computeMonthlyVisitData(visits: List<RawVisit>): MonthlyVisitData {
    val tallies = LinkedHashMap<String, MerchantVisitTally>()
    for (visit in visits) {
        if (visit.isAdHoc == true) continue
        val tally = tallies.getOrPut(visit.merchantId) { MerchantVisitTally() }
        tally.visits += 1
        if (visit.visitType == WITH_ORDER) tally.visitsWithOrder += 1
    }

    var totalPlanned = 0
    var totalOrders = 0
    for (tally in tallies.values) {
        val cappedVisits = minOf(tally.visits, VISITS_PER_MERCHANT_PER_MONTH)
        totalPlanned += cappedVisits
        totalOrders += minOf(tally.visitsWithOrder, cappedVisits)
    }
    return MonthlyVisitData(totalPlanned, totalOrders)
}

/**
 * Week-based visit data: per merchant, the number of week chunks (0–3) with at
 * least one visit, and the number of week chunks with at least one order.
 * Ad-hoc visits are NOT filtered here (same as the PWA). Visit days are read
 * in [zone], the device zone in the PWA.
 */
fun computeWeekBasedVisitData(
    visits: List<RawVisit>,
    referenceDate: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): WeekBasedVisitData {
    val weeksElapsed = getWeeksElapsed(referenceDate)
    // A null week stands for the PWA's NaN week of an Invalid Date: one extra bucket per merchant.
    val coveredWeeks = LinkedHashMap<String, MutableSet<Int?>>()
    val orderWeeks = LinkedHashMap<String, MutableSet<Int?>>()

    for (visit in visits) {
        val weekIndex = parseJsDateMillis(visit.visitDate, zone)
            ?.let { getWeekIndex(Instant.ofEpochMilli(it).atZone(zone).dayOfMonth) }
        coveredWeeks.getOrPut(visit.merchantId) { mutableSetOf() } += weekIndex
        val orders = orderWeeks.getOrPut(visit.merchantId) { mutableSetOf() }
        if (visit.visitType == WITH_ORDER) orders += weekIndex
    }

    return WeekBasedVisitData(
        totalPlanned = coveredWeeks.values.sumOf { it.size },
        totalOrders = orderWeeks.values.sumOf { it.size },
        weeksElapsed = weeksElapsed,
    )
}

/** Route performance KPIs. Rates are percentages rounded to one decimal. */
data class RouteKPIs(
    val totalMerchants: Int,
    /** totalMerchants × [VISITS_PER_MERCHANT_PER_MONTH]. */
    val totalPlannedVisits: Int,
    val plannedVisitsCompleted: Int,
    val visitsWithOrder: Int,
    /** Alias of [plannedVisitsCompleted] (display compat). */
    val totalVisits: Int,
    /** Alias of [totalPlannedVisits] (display compat). */
    val expectedVisits: Int,
    val strikeRate: Double,
    val completionRate: Double,
    val todayVisits: Int,
    val dailyVisitTarget: Int,
)

/**
 * Completion rate = completed planned visits ÷ monthly target × 100 (capped at
 * the target). Strike rate = (capped regular orders + ad-hoc orders) ÷ completed
 * planned visits × 100. The daily target paces the full monthly target over the
 * working days of the current month in [zone] (device zone in the PWA).
 */
fun calculateKPIs(
    merchants: List<RouteMerchant>,
    todayVisitCount: Int,
    visitsData: VisitsData,
    adhocVisitOrderCount: Int? = null,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): RouteKPIs {
    val totalMerchants = merchants.size
    val totalPlannedVisits = totalMerchants * VISITS_PER_MERCHANT_PER_MONTH
    val cappedCompletedVisits = minOf(visitsData.totalPlanned, totalPlannedVisits)

    val completionRate = if (totalPlannedVisits > 0) {
        cappedCompletedVisits.toDouble() / totalPlannedVisits * 100
    } else {
        0.0
    }

    val adhocOrders = adhocVisitOrderCount ?: 0
    val cappedRegularOrders = minOf(visitsData.totalOrders, cappedCompletedVisits)
    val strikeRate = if (cappedCompletedVisits > 0) {
        (cappedRegularOrders + adhocOrders).toDouble() / cappedCompletedVisits * 100
    } else {
        0.0
    }

    val daysInMonth = YearMonth.from(now.atZone(zone)).lengthOfMonth()
    val workingDays = daysInMonth - VACATION_DAYS_PER_MONTH
    val fullMonthTarget = totalMerchants * VISITS_PER_MERCHANT_PER_MONTH
    val dailyVisitTarget = if (fullMonthTarget > 0 && workingDays > 0) {
        ceil(fullMonthTarget.toDouble() / workingDays).toInt()
    } else {
        0
    }

    return RouteKPIs(
        totalMerchants = totalMerchants,
        totalPlannedVisits = totalPlannedVisits,
        plannedVisitsCompleted = cappedCompletedVisits,
        visitsWithOrder = cappedRegularOrders + adhocOrders,
        totalVisits = cappedCompletedVisits,
        expectedVisits = totalPlannedVisits,
        strikeRate = jsMathRound(strikeRate * 10) / 10,
        completionRate = jsMathRound(completionRate * 10) / 10,
        todayVisits = todayVisitCount,
        dailyVisitTarget = dailyVisitTarget,
    )
}

fun getEmptyKPIs(): RouteKPIs = RouteKPIs(
    totalMerchants = 0,
    totalPlannedVisits = 0,
    plannedVisitsCompleted = 0,
    visitsWithOrder = 0,
    totalVisits = 0,
    expectedVisits = 0,
    strikeRate = 0.0,
    completionRate = 0.0,
    todayVisits = 0,
    dailyVisitTarget = 0,
)
