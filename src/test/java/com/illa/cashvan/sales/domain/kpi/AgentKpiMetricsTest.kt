package com.illa.cashvan.sales.domain.kpi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

// Ported from the PWA's src/utils/__tests__/agentKpiMetrics.test.ts and
// src/hooks/__tests__/useAgentKpiMetrics.test.ts (all pure cases).
class AgentKpiMetricsTest {

    private val response = AgentKpiMobileResponse(
        agent = AgentKpiAgent(
            id = "8c23d557-2cbb-4bf8-b844-d36cbf97765b",
            name = "Test Agent",
            agentCode = "ALEX_0018",
            phoneNumber = "01002082868",
            presellAgentId = "8c23d557-2cbb-4bf8-b844-d36cbf97765b",
            cashvanAgentId = "f4e1f4e1-f4e1-4f4e-9f4e-f4e1f4e1f4e1",
        ),
        period = AgentKpiPeriod(month = 5, year = 2026, isCurrentMonth = true),
        todayVisits = AgentKpiTodayVisits(actual = 12, target = 14, ratio = "12/14", percentage = 85.7),
        completionRate = AgentKpiCompletionRate(actual = 28, expected = 128, ratio = "28/128", percentage = 21.9),
        strikeRate = AgentKpiStrikeRate(withOrder = 30, completed = 128, ratio = "30/128", percentage = 23.4),
        totals = AgentKpiTotals(
            presellVisits = 0,
            cashvanVisits = 28,
            totalVisits = 28,
            totalOrders = 28,
            totalOrderValue = 32092.6,
            totalMerchants = 64,
        ),
        adHocVisits = AgentKpiAdHocVisits(total = 13, withOrder = 13),
    )

    private val afterTwoPmCairo = Instant.parse("2026-06-25T12:00:00.000Z")
    private val beforeTwoPmCairo = Instant.parse("2026-06-25T10:59:59.000Z")

    private fun withPercentages(today: Double? = null, completion: Double? = null, strike: Double? = null) =
        response.copy(
            todayVisits = response.todayVisits.copy(percentage = today ?: response.todayVisits.percentage),
            completionRate = response.completionRate.copy(percentage = completion ?: response.completionRate.percentage),
            strikeRate = response.strikeRate.copy(percentage = strike ?: response.strikeRate.percentage),
        )

    private val logVisits = AgentKpiMessageData(AgentKpiMessageKey.LOG_VISITS, "kpi.messages.logVisits")
    private val dailyTarget = AgentKpiMessageData(AgentKpiMessageKey.DAILY_TARGET, "kpi.messages.dailyTarget")

    // getAgentFirstName

    @Test
    fun `extracts the first name from a full name, ignoring surrounding and repeated whitespace`() {
        assertEquals("Ahmed", getAgentFirstName("Ahmed Mohamed Ismail"))
        assertEquals("Ahmed", getAgentFirstName("  Ahmed   Mohamed Ismail  "))
        assertEquals("Ahmed", getAgentFirstName("Ahmed"))
        assertEquals("أحمد", getAgentFirstName("أحمد محمد إسماعيل"))
    }

    @Test
    fun `returns an empty first name for blank or missing names`() {
        assertEquals("", getAgentFirstName(""))
        assertEquals("", getAgentFirstName("   "))
        assertEquals("", getAgentFirstName(null))
    }

    @Test
    fun `splits on any JS whitespace, including no-break spaces (extra)`() {
        assertEquals("Ahmed", getAgentFirstName(" Ahmed Mohamed"))
        assertEquals("Ahmed", getAgentFirstName("Ahmed\tMohamed"))
    }

    // mapAgentKpiResponseToCards

    @Test
    fun `uses server percentages and ratios without recomputing them`() {
        assertEquals(
            listOf(
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
            ),
            mapAgentKpiResponseToCards(response),
        )
    }

    @Test
    fun `keeps returned zero-denominator ratios as display text`() {
        val cards = mapAgentKpiResponseToCards(
            response.copy(
                todayVisits = AgentKpiTodayVisits(actual = 0, target = 0, ratio = "0/0", percentage = 0.0),
                completionRate = AgentKpiCompletionRate(actual = 0, expected = 0, ratio = "0/0", percentage = 0.0),
                strikeRate = AgentKpiStrikeRate(withOrder = 0, completed = 0, ratio = "0/0", percentage = 0.0),
            ),
        )

        assertEquals(listOf("0.0%", "0.0%", "0/0"), cards.map { it.displayValue })
        assertEquals(listOf(0.0, 0.0, 0.0), cards.map { it.percentage })
        assertEquals(listOf("0/0", "0/0", null), cards.map { it.subtitle })
    }

    @Test
    fun `formats percentages with one decimal and NaN as zero (extra)`() {
        val cards = mapAgentKpiResponseToCards(withPercentages(completion = Double.NaN, strike = 66.66))

        assertEquals("66.7%", cards[0].displayValue)
        assertEquals("0.0%", cards[1].displayValue)
    }

    // getAgentKpiErrorMessageKey

    @Test
    fun `maps documented function status codes to user-facing translation keys`() {
        assertEquals("kpi.errors.agentNotFound", getAgentKpiErrorMessageKey(404))
        assertEquals("kpi.errors.accountNotLinked", getAgentKpiErrorMessageKey(409))
        assertEquals("kpi.errors.generic", getAgentKpiErrorMessageKey(500))
        assertEquals("kpi.errors.generic", getAgentKpiErrorMessageKey())
    }

    // getAgentKpiMessages

    @Test
    fun `shows the visit logging message when monthly visits are low but successful visits are high`() {
        assertEquals(listOf(logVisits), getAgentKpiMessages(withPercentages(completion = 64.9, strike = 80.1)))
    }

    @Test
    fun `does not show the visit logging message on the boundary values`() {
        assertEquals(
            emptyList<AgentKpiMessageData>(),
            getAgentKpiMessages(withPercentages(completion = 65.0, strike = 80.0), afterTwoPmCairo),
        )
    }

    @Test
    fun `shows the daily target message only after 2 PM Egypt time`() {
        assertEquals(listOf(dailyTarget), getAgentKpiMessages(withPercentages(today = 59.9), afterTwoPmCairo))
    }

    @Test
    fun `does not show the daily target message before 2 PM Egypt time`() {
        assertEquals(
            emptyList<AgentKpiMessageData>(),
            getAgentKpiMessages(withPercentages(today = 59.9), beforeTwoPmCairo),
        )
    }

    @Test
    fun `returns both messages when both KPI conditions are true`() {
        val messages = getAgentKpiMessages(withPercentages(today = 50.0, completion = 50.0, strike = 90.0), afterTwoPmCairo)

        assertEquals(listOf(AgentKpiMessageKey.LOG_VISITS, AgentKpiMessageKey.DAILY_TARGET), messages.map { it.key })
    }

    // getAgentKpiMessageKeySignature

    @Test
    fun `keeps the same signature for new arrays with the same message keys`() {
        val first = listOf(logVisits, dailyTarget)
        val second = first.map { it.copy() }

        assertEquals(getAgentKpiMessageKeySignature(first), getAgentKpiMessageKeySignature(second))
        assertEquals("logVisits,dailyTarget", getAgentKpiMessageKeySignature(first))
    }

    @Test
    fun `changes the signature when message keys change`() {
        assertNotEquals(
            getAgentKpiMessageKeySignature(listOf(logVisits)),
            getAgentKpiMessageKeySignature(listOf(dailyTarget)),
        )
    }

    // shouldRetryAgentKpiQuery

    @Test
    fun `does not retry documented account or configuration errors`() {
        assertFalse(shouldRetryAgentKpiQuery(0, getFunctionErrorStatus(contextStatus = 404, status = null)))
        assertFalse(shouldRetryAgentKpiQuery(0, getFunctionErrorStatus(contextStatus = 409, status = null)))
    }

    @Test
    fun `retries transient or server failures up to three attempts`() {
        assertTrue(shouldRetryAgentKpiQuery(0, getFunctionErrorStatus(contextStatus = 500, status = null)))
        // new Error('network') carries no status.
        assertTrue(shouldRetryAgentKpiQuery(2, getFunctionErrorStatus(contextStatus = null, status = null)))
        assertFalse(shouldRetryAgentKpiQuery(3, getFunctionErrorStatus(contextStatus = null, status = null)))
    }

    @Test
    fun `prefers the context status over the top-level status (extra)`() {
        assertEquals(404, getFunctionErrorStatus(contextStatus = 404, status = 500))
        assertEquals(409, getFunctionErrorStatus(contextStatus = null, status = 409))
    }
}
