package com.illa.cashvan.sales.feature.kpi.data.mapper

import com.illa.cashvan.sales.domain.kpi.AgentKpiAdHocVisits
import com.illa.cashvan.sales.domain.kpi.AgentKpiAgent
import com.illa.cashvan.sales.domain.kpi.AgentKpiCompletionRate
import com.illa.cashvan.sales.domain.kpi.AgentKpiMobileResponse
import com.illa.cashvan.sales.domain.kpi.AgentKpiPeriod
import com.illa.cashvan.sales.domain.kpi.AgentKpiStrikeRate
import com.illa.cashvan.sales.domain.kpi.AgentKpiTodayVisits
import com.illa.cashvan.sales.domain.kpi.AgentKpiTotals
import com.illa.cashvan.sales.domain.kpi.CommissionPayoutRow
import com.illa.cashvan.sales.domain.kpi.KpiAgentSummaryRow
import com.illa.cashvan.shared.feature.kpi.data.model.AgentKpiResponse
import com.illa.cashvan.sales.feature.kpi.data.model.CommissionPayoutRow as CommissionPayoutDto
import com.illa.cashvan.sales.feature.kpi.data.model.KpiAgentSummaryRow as KpiAgentSummaryDto

fun AgentKpiResponse.toDomain(): AgentKpiMobileResponse = AgentKpiMobileResponse(
    agent = AgentKpiAgent(agent.id, agent.name, agent.agent_code, agent.phone_number, agent.presell_agent_id, agent.cashvan_agent_id),
    period = AgentKpiPeriod(period.month, period.year, period.is_current_month),
    todayVisits = AgentKpiTodayVisits(today_visits.actual, today_visits.target, today_visits.ratio, today_visits.percentage),
    completionRate = AgentKpiCompletionRate(completion_rate.actual, completion_rate.expected, completion_rate.ratio, completion_rate.percentage),
    strikeRate = AgentKpiStrikeRate(strike_rate.with_order, strike_rate.completed, strike_rate.ratio, strike_rate.percentage),
    totals = AgentKpiTotals(
        totals.presell_visits, totals.cashvan_visits, totals.total_visits, totals.total_orders,
        totals.total_order_value, totals.total_merchants
    ),
    adHocVisits = AgentKpiAdHocVisits(ad_hoc_visits.total, ad_hoc_visits.with_order)
)

fun KpiAgentSummaryDto.toDomain(): KpiAgentSummaryRow = KpiAgentSummaryRow(
    agentId = agent_id,
    agentName = agent_name.orEmpty(),
    agentCode = "",
    totalVisits = total_visits,
    visitsWithOrder = visits_with_order,
    uniqueVisits = unique_visits,
    totalMerchants = total_merchants,
    completionRate = 0.0,
    totalOrderValue = total_order_value ?: 0.0,
    strikeRate = 0.0
)

fun CommissionPayoutDto.toDomain(): CommissionPayoutRow =
    CommissionPayoutRow(target_egp, actual_egp, payout_egp, payout_at_threshold_egp, min_threshold_pct, snapshotted_at)
