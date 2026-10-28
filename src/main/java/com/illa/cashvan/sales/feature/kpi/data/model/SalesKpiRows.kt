package com.illa.cashvan.sales.feature.kpi.data.model

import com.illa.cashvan.core.network.util.FlexibleDoubleSerializer
import kotlinx.serialization.Serializable

/** A row of `get_kpi_agents_summary`, which returns every agent for the month. */
@Serializable
data class KpiAgentSummaryRow(
    val agent_id: String,
    val agent_name: String? = null,
    val total_visits: Int = 0,
    val visits_with_order: Int = 0,
    val unique_visits: Int = 0,
    val total_merchants: Int = 0,
    @Serializable(with = FlexibleDoubleSerializer::class) val total_order_value: Double? = null
)

@Serializable
data class AgentIdRow(
    val id: String
)

/** `get_agent_route_period_metrics` for one period, optionally one route. */
@Serializable
data class RoutePeriodMetricsRow(
    val total_merchants: Int = 0,
    val unique_merchants_visited: Int = 0,
    val planned_completed: Int = 0,
    val visits_with_order: Int = 0,
    val ad_hoc_with_order: Int = 0,
    @Serializable(with = FlexibleDoubleSerializer::class) val strike_rate: Double? = null
)

@Serializable
data class VisitDayRow(
    val visit_date: String,
    val is_ad_hoc: Boolean? = null
)

@Serializable
data class NoOrderReasonIdRow(
    val no_order_reason_id: String? = null
)

@Serializable
data class ReasonTextRow(
    val reason_ar: String? = null
)

@Serializable
data class CommissionPayoutRow(
    @Serializable(with = FlexibleDoubleSerializer::class) val target_egp: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val actual_egp: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val payout_egp: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val payout_at_threshold_egp: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val min_threshold_pct: Double? = null,
    val snapshotted_at: String? = null
)

@Serializable
data class AssignedRouteRow(
    val route_id: String,
    val routes: AssignedRouteRef? = null
)

@Serializable
data class AssignedRouteRef(
    val id: String,
    val name: String? = null,
    val color: String? = null
)

@Serializable
data class AgentKpiRequestBody(
    val agent_id: String,
    val agent_type: String,
    val month: Int? = null,
    val year: Int? = null
)
