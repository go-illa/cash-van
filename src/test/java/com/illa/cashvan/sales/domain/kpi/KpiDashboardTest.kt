package com.illa.cashvan.sales.domain.kpi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported from the PWA's src/components/dashboard/AgentKpiSection.test.tsx and
// KPICards.test.tsx: the markup assertions become assertions on the banner,
// subtitle and card helpers. As in the TS tests, translation is the identity
// (keys render as themselves) unless a test checks the Arabic copy.
class KpiDashboardTest {

    private val identity: (String) -> String = { it }

    private val cards = listOf(
        AgentKpiCardData(
            key = AgentKpiCardKey.STRIKE_RATE,
            labelKey = "route.strikeRate",
            displayValue = "23.4%",
            percentage = 23.4,
            subtitle = "30/128",
        ),
        AgentKpiCardData(
            key = AgentKpiCardKey.COMPLETION_RATE,
            labelKey = "route.completionRate",
            displayValue = "21.9%",
            percentage = 21.9,
            subtitle = "28/128",
        ),
        AgentKpiCardData(
            key = AgentKpiCardKey.TODAY_VISITS,
            labelKey = "route.todayVisits",
            displayValue = "12/14",
            percentage = 85.7,
            subtitleKey = "route.dailyTarget",
        ),
    )

    private val logVisits = AgentKpiMessageData(AgentKpiMessageKey.LOG_VISITS, "kpi.messages.logVisits")
    private val dailyTarget = AgentKpiMessageData(AgentKpiMessageKey.DAILY_TARGET, "kpi.messages.dailyTarget")

    private fun banner(messages: List<AgentKpiMessageData>, agentName: String? = null, activeIndex: Int = 0) =
        kpiEncouragementBanner(messages, activeIndex, agentName, identity)

    // AgentKpiSection

    @Test
    fun `does not render an encouragement banner when no messages are active`() {
        assertNull(banner(emptyList()))
    }

    @Test
    fun `renders the active encouragement message with the first-name greeting`() {
        val shown = banner(listOf(logVisits), agentName = "  Ahmed   Mohamed Ismail  ")

        assertEquals(KpiEncouragementBanner(AgentKpiMessageKey.LOG_VISITS, "يا Ahmed، ", "kpi.messages.logVisits"), shown)
        assertFalse(shown.toString().contains("Mohamed"))
    }

    @Test
    fun `renders one shared banner location when multiple messages are active`() {
        val shown = banner(listOf(logVisits, dailyTarget))

        assertEquals("kpi.messages.logVisits", shown?.message)
        assertTrue(shouldRotateKpiMessages(2))
    }

    @Test
    fun `personalizes any encouragement message and falls back cleanly without a name`() {
        val personalized = banner(listOf(dailyTarget), agentName = "Ahmed Mohamed Ismail")
        val fallback = banner(listOf(dailyTarget), agentName = "   ")

        assertEquals("يا Ahmed، ", personalized?.greeting)
        assertEquals("kpi.messages.dailyTarget", personalized?.message)
        assertEquals("kpi.messages.dailyTarget", fallback?.message)
        assertNull(fallback?.greeting)
    }

    @Test
    fun `rotates through messages and falls back to the first for a stale index (extra)`() {
        val messages = listOf(logVisits, dailyTarget)

        assertFalse(shouldRotateKpiMessages(1))
        assertEquals(1, nextKpiMessageIndex(0, 2))
        assertEquals(0, nextKpiMessageIndex(1, 2))
        assertEquals(AgentKpiMessageKey.DAILY_TARGET, banner(messages, activeIndex = 1)?.key)
        assertEquals(AgentKpiMessageKey.LOG_VISITS, banner(listOf(logVisits), activeIndex = 1)?.key)
        assertEquals(9_000L, KPI_MESSAGE_ROTATION_MS)
    }

    @Test
    fun `renders the Arabic copy by default (extra)`() {
        val shown = kpiEncouragementBanner(listOf(logVisits), 0, "أحمد علي")

        assertEquals("يا أحمد، ", shown?.greeting)
        assertEquals("شغلك في البيع ممتاز، بس محتاجين نسجل كل زيارة حتى لو التاجر مش عايز يطلب.", shown?.message)
        assertEquals("المؤشرات غير متاحة. لم يتم العثور على المندوب، تواصل مع المشرف.", translateKpiKey(getAgentKpiErrorMessageKey(404)))
        assertEquals("route.unknown", translateKpiKey("route.unknown"))
    }

    @Test
    fun `replaces the completion card with real coverage once it loads (extra)`() {
        val coverage = computeAgentCoverage(
            listOf(
                KpiAgentSummaryRow("other", "Other", "SA-2", 1, 1, 1, 1, 0.0, 0.0, 0.0),
                KpiAgentSummaryRow("me", "Me", "SA-1", 30, 10, 13, 20, 0.0, 0.0, 0.0),
            ),
            agentId = "me",
        )
        val displayed = applyAgentCoverage(cards, coverage)

        assertEquals(AgentCoverage(uniqueVisited = 13, totalMerchants = 20, percentage = 65.0), coverage)
        assertEquals(cards[0], displayed[0])
        assertEquals(
            cards[1].copy(labelKey = "route.completionRateCoverage", displayValue = "13/20", percentage = 65.0, subtitle = "65% التغطية"),
            displayed[1],
        )
        assertEquals(cards[2], displayed[2])
        assertEquals(cards, applyAgentCoverage(cards, null))
    }

    @Test
    fun `caps coverage at 100 percent and is zero without assigned merchants (extra)`() {
        val over = computeAgentCoverage(listOf(KpiAgentSummaryRow("me", "Me", "SA-1", 0, 0, 25, 20, 0.0, 0.0, 0.0)), "me")

        assertEquals(100.0, over.percentage, 0.0)
        assertEquals(AgentCoverage(0, 0, 0.0), computeAgentCoverage(emptyList(), "me"))
    }

    @Test
    fun `routes each KPI card to its detail page (extra)`() {
        assertEquals(KpiDestination.VISITS, kpiCardDestination(AgentKpiCardKey.TODAY_VISITS))
        assertEquals(KpiDestination.COVERAGE, kpiCardDestination(AgentKpiCardKey.COMPLETION_RATE))
        assertEquals(KpiDestination.CONVERSION, kpiCardDestination(AgentKpiCardKey.STRIKE_RATE))
    }

    // KPICards

    @Test
    fun `renders server-provided KPI values and ratios`() {
        val subtitles = cards.map { resolveKpiCardSubtitle(it, identity) }

        assertEquals(listOf("23.4%", "21.9%", "12/14"), cards.map { it.displayValue })
        assertEquals(listOf("30/128", "28/128", "route.dailyTarget"), subtitles)
        assertFalse(subtitles.contains("30/128 route.visits"))
    }

    @Test
    fun `appends the translated subtitle suffix and translates subtitle keys (extra)`() {
        val withSuffix = cards[0].copy(subtitleSuffixKey = "route.visits")

        assertEquals("30/128 route.visits", resolveKpiCardSubtitle(withSuffix, identity))
        assertEquals("هدف يومي", resolveKpiCardSubtitle(cards[2]))
        assertEquals("", resolveKpiCardSubtitle(cards[0].copy(subtitle = null)))
    }

    @Test
    fun `colours cards by status thresholds (extra)`() {
        assertEquals(KpiStatusTone.DANGER, kpiStatusTone(49.9))
        assertEquals(KpiStatusTone.PROGRESS, kpiStatusTone(50.0))
        assertEquals(KpiStatusTone.PROGRESS, kpiStatusTone(74.9))
        assertEquals(KpiStatusTone.SUCCESS, kpiStatusTone(75.0))
        assertEquals(KpiStatusTone.SUCCESS, kpiStatusTone(Double.NaN))
    }

    @Test
    fun `labels and fills the ring like the PWA (extra)`() {
        assertEquals("86%", kpiRingLabel(85.7))
        assertEquals("23%", kpiRingLabel(23.4))
        assertEquals("1%", kpiRingLabel(0.5))
        assertEquals(0.857, kpiRingFraction(85.7), 1e-12)
        assertEquals(1.0, kpiRingFraction(140.0), 0.0)
        assertEquals("شاهد المزيد", KPI_CARD_SEE_MORE_LABEL)
    }
}
