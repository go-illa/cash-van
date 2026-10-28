package com.illa.cashvan.sales.domain.kpi

import org.junit.Assert.assertEquals
import org.junit.Test

// Ported from the PWA's src/pages/LeaderboardPage.test.tsx (the eligibility
// filter and podium/list formatting; query wiring, auth gating and skeleton
// markup are not ported), plus extra cases for ranking and the footer.
class LeaderboardTest {

    private fun row(
        agentId: String,
        totalVisits: Int = 0,
        visitsWithOrder: Int = 0,
        uniqueVisits: Int = 0,
        totalMerchants: Int = 10,
        totalOrderValue: Double = 0.0,
    ) = KpiAgentSummaryRow(
        agentId = agentId,
        agentName = agentId,
        agentCode = "SA",
        totalVisits = totalVisits,
        visitsWithOrder = visitsWithOrder,
        uniqueVisits = uniqueVisits,
        totalMerchants = totalMerchants,
        completionRate = 0.0,
        totalOrderValue = totalOrderValue,
        strikeRate = 0.0,
    )

    @Test
    fun `returns only active sales agents and preserves their KPI calculations`() {
        val active = KpiAgentSummaryRow(
            agentId = "active-sales-agent",
            agentName = "Active Sales",
            agentCode = "SA-1",
            totalVisits = 8,
            visitsWithOrder = 3,
            uniqueVisits = 4,
            totalMerchants = 10,
            completionRate = 50.0,
            totalOrderValue = 5000.0,
            strikeRate = 37.5,
        )
        val rows = listOf(
            active,
            row("delivery-agent", totalVisits = 12, uniqueVisits = 7, totalOrderValue = 7000.0),
            row("inactive-sales-agent", totalVisits = 6, uniqueVisits = 3, totalOrderValue = 3000.0),
        )

        assertEquals(listOf(active), filterLeaderboardRows(rows, setOf("active-sales-agent")))
    }

    @Test
    fun `uses compact M values on the podium while keeping exact EGP values in the list`() {
        val top = row("agent-1", totalVisits = 20, visitsWithOrder = 10, uniqueVisits = 12, totalMerchants = 20, totalOrderValue = 1_590_700.0)

        assertEquals("1.59M", leaderboardPodiumValue(top, LeaderboardTab.SALES))
        assertEquals("1,590,700 ج", leaderboardListValue(top, LeaderboardTab.SALES))
    }

    @Test
    fun `sorts each tab by its own metric, highest first and stable on ties (extra)`() {
        val a = row("a", totalVisits = 5, uniqueVisits = 9, totalMerchants = 10, totalOrderValue = 100.0)
        val b = row("b", totalVisits = 9, uniqueVisits = 3, totalMerchants = 4, totalOrderValue = 300.0)
        val c = row("c", totalVisits = 9, uniqueVisits = 0, totalMerchants = 0, totalOrderValue = 200.0)
        val rows = listOf(a, b, c)

        assertEquals(listOf(b, c, a), sortLeaderboard(rows, LeaderboardTab.SALES))
        assertEquals(listOf(b, c, a), sortLeaderboard(rows, LeaderboardTab.VISITS))
        // 90% > 75% > 0% (no assigned merchants).
        assertEquals(listOf(a, b, c), sortLeaderboard(rows, LeaderboardTab.COVERAGE))
    }

    @Test
    fun `orders the podium 2nd, 1st, 3rd and keeps real ranks with fewer agents (extra)`() {
        val first = row("first")
        val second = row("second")
        val third = row("third")

        assertEquals(
            listOf(LeaderboardPodiumEntry(second, 2), LeaderboardPodiumEntry(first, 1), LeaderboardPodiumEntry(third, 3)),
            leaderboardPodium(listOf(first, second, third, row("fourth"))),
        )
        assertEquals(listOf(LeaderboardPodiumEntry(first, 1)), leaderboardPodium(listOf(first)))
        assertEquals(emptyList<LeaderboardPodiumEntry>(), leaderboardPodium(emptyList()))
    }

    @Test
    fun `formats coverage and visits values (extra)`() {
        val agent = row("a", totalVisits = 17, uniqueVisits = 2, totalMerchants = 3)

        assertEquals("67%", leaderboardPodiumValue(agent, LeaderboardTab.COVERAGE))
        assertEquals("67%", leaderboardListValue(agent, LeaderboardTab.COVERAGE))
        assertEquals("17", leaderboardListValue(agent, LeaderboardTab.VISITS))
        assertEquals("0%", leaderboardListValue(row("b", totalMerchants = 0), LeaderboardTab.COVERAGE))
        assertEquals("950K", leaderboardPodiumValue(row("c", totalOrderValue = 950_000.0), LeaderboardTab.SALES))
    }

    @Test
    fun `describes the sort in the footer (extra)`() {
        assertEquals("3 مندوب · المبيعات بالجنيه", leaderboardFooter(3, LeaderboardTab.SALES))
        assertEquals("3 مندوب · مرتّب حسب التغطية", leaderboardFooter(3, LeaderboardTab.COVERAGE))
        assertEquals("0 مندوب · مرتّب حسب الزيارات", leaderboardFooter(0, LeaderboardTab.VISITS))
    }

    @Test
    fun `lists tabs in the PWA order with sales first (extra)`() {
        assertEquals(listOf("المبيعات", "التغطية", "الزيارات"), LeaderboardTab.entries.map { it.label })
    }
}
