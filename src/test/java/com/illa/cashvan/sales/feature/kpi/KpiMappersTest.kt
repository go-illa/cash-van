package com.illa.cashvan.sales.feature.kpi

import com.illa.cashvan.sales.domain.kpi.computeAgentCoverage
import com.illa.cashvan.sales.feature.kpi.data.mapper.toDomain
import com.illa.cashvan.sales.feature.kpi.data.model.KpiAgentSummaryRow
import org.junit.Assert.assertEquals
import org.junit.Test

class KpiMappersTest {

    @Test
    fun `summary rows feed the agent's coverage`() {
        val rows = listOf(
            KpiAgentSummaryRow(agent_id = "me", agent_name = "Ali", unique_visits = 30, total_merchants = 120, total_order_value = 5000.0),
            KpiAgentSummaryRow(agent_id = "other", unique_visits = 10, total_merchants = 10)
        ).map { it.toDomain() }
        val coverage = computeAgentCoverage(rows, "me")
        assertEquals(30, coverage.uniqueVisited)
        assertEquals(120, coverage.totalMerchants)
        assertEquals(25.0, coverage.percentage, 0.01)
        assertEquals(5000.0, rows.first().totalOrderValue, 0.0)
    }
}
