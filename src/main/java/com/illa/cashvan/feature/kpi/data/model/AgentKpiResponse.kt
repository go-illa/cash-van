package com.illa.cashvan.feature.kpi.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AgentKpiResponse(
    val agent: KpiAgent,
    val period: KpiPeriod,
    val today_visits: TodayVisits,
    val completion_rate: CompletionRate,
    val strike_rate: StrikeRate,
    val totals: KpiTotals,
    val ad_hoc_visits: AdHocVisits
)

@Serializable
data class KpiAgent(
    val id: String,
    val name: String? = null,
    val agent_code: String? = null,
    val phone_number: String? = null,
    val presell_agent_id: String,
    val cashvan_agent_id: String? = null
)

@Serializable
data class KpiPeriod(
    val month: Int,
    val year: Int,
    val is_current_month: Boolean
)

@Serializable
data class TodayVisits(
    val actual: Int,
    val target: Int,
    val ratio: String,
    val percentage: Double
)

@Serializable
data class CompletionRate(
    val actual: Int,
    val expected: Int,
    val ratio: String,
    val percentage: Double
)

@Serializable
data class StrikeRate(
    val with_order: Int,
    val completed: Int,
    val ratio: String,
    val percentage: Double
)

@Serializable
data class KpiTotals(
    val presell_visits: Int,
    val cashvan_visits: Int,
    val total_visits: Int,
    val total_orders: Int,
    val total_order_value: Double
)

@Serializable
data class AdHocVisits(
    val total: Int,
    val with_order: Int
)
