package com.illa.cashvan.sales.domain.kpi

import com.illa.cashvan.sales.domain.format.formatCompactSales
import com.illa.cashvan.sales.domain.format.formatEGP
import com.illa.cashvan.sales.domain.format.formatPercent

// Ranking and formatting logic of the PWA's src/pages/LeaderboardPage.tsx.

/** One row of the `get_kpi_agents_summary` RPC. */
data class KpiAgentSummaryRow(
    val agentId: String,
    val agentName: String,
    val agentCode: String,
    val totalVisits: Int,
    val visitsWithOrder: Int,
    /** Distinct assigned merchants visited this month. */
    val uniqueVisits: Int,
    val totalMerchants: Int,
    val completionRate: Double,
    val totalOrderValue: Double,
    val strikeRate: Double,
)

/** Tabs in display order; [SALES] is the default. */
enum class LeaderboardTab(val label: String) {
    SALES("المبيعات"),
    COVERAGE("التغطية"),
    VISITS("الزيارات"),
}

/** The three podium ranks in display order: 2nd, 1st (centre, elevated), 3rd. */
data class LeaderboardPodiumEntry(val row: KpiAgentSummaryRow, val rank: Int)

/** Coverage % = unique merchants visited ÷ total assigned merchants. */
fun leaderboardCoveragePct(row: KpiAgentSummaryRow): Double =
    if (row.totalMerchants > 0) row.uniqueVisits.toDouble() / row.totalMerchants * 100 else 0.0

/**
 * Only active sales agents are eligible: delivery and inactive agents stay in
 * the shared RPC for other consumers. If the eligibility lookup fails the PWA
 * fails closed (errors) rather than showing unfiltered rows.
 */
fun filterLeaderboardRows(rows: List<KpiAgentSummaryRow>, activeSalesAgentIds: Set<String>): List<KpiAgentSummaryRow> =
    rows.filter { it.agentId in activeSalesAgentIds }

private fun leaderboardMetric(row: KpiAgentSummaryRow, tab: LeaderboardTab): Double = when (tab) {
    LeaderboardTab.COVERAGE -> leaderboardCoveragePct(row)
    LeaderboardTab.VISITS -> row.totalVisits.toDouble()
    LeaderboardTab.SALES -> row.totalOrderValue
}

/** Highest first; stable, so ties keep the RPC order (JS `Array.prototype.sort` with `b - a`). */
fun sortLeaderboard(rows: List<KpiAgentSummaryRow>, tab: LeaderboardTab): List<KpiAgentSummaryRow> =
    rows.sortedWith { a, b ->
        val difference = leaderboardMetric(b, tab) - leaderboardMetric(a, tab)
        // A NaN difference compares as equal, as in JS.
        when {
            difference > 0 -> 1
            difference < 0 -> -1
            else -> 0
        }
    }

/** Podium of the already-sorted rows: each agent keeps its real rank even with fewer than three agents. */
fun leaderboardPodium(sorted: List<KpiAgentSummaryRow>): List<LeaderboardPodiumEntry> = listOfNotNull(
    sorted.getOrNull(1)?.let { LeaderboardPodiumEntry(it, 2) },
    sorted.getOrNull(0)?.let { LeaderboardPodiumEntry(it, 1) },
    sorted.getOrNull(2)?.let { LeaderboardPodiumEntry(it, 3) },
)

/** Podium value: compact sales ("1.59M") so it fits under the trophy. */
fun leaderboardPodiumValue(row: KpiAgentSummaryRow, tab: LeaderboardTab): String = when (tab) {
    LeaderboardTab.COVERAGE -> formatPercent(leaderboardCoveragePct(row))
    LeaderboardTab.VISITS -> row.totalVisits.toString()
    LeaderboardTab.SALES -> formatCompactSales(row.totalOrderValue)
}

/** Ranked-list value: exact EGP for sales ("1,590,700 ج"). */
fun leaderboardListValue(row: KpiAgentSummaryRow, tab: LeaderboardTab): String = when (tab) {
    LeaderboardTab.COVERAGE -> formatPercent(leaderboardCoveragePct(row))
    LeaderboardTab.VISITS -> row.totalVisits.toString()
    LeaderboardTab.SALES -> formatEGP(row.totalOrderValue)
}

/** Footer under the list, e.g. "12 مندوب · المبيعات بالجنيه" or "12 مندوب · مرتّب حسب التغطية". */
fun leaderboardFooter(agentCount: Int, tab: LeaderboardTab): String {
    val sortNote = if (tab == LeaderboardTab.SALES) "المبيعات بالجنيه" else "مرتّب حسب ${tab.label}"
    return "$agentCount مندوب · $sortNote"
}
