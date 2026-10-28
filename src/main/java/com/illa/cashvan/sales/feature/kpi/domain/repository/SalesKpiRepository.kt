package com.illa.cashvan.sales.feature.kpi.domain.repository

import com.illa.cashvan.sales.feature.kpi.data.model.AssignedRouteRef
import com.illa.cashvan.sales.feature.kpi.data.model.CommissionPayoutRow
import com.illa.cashvan.sales.feature.kpi.data.model.KpiAgentSummaryRow
import com.illa.cashvan.sales.feature.kpi.data.model.RoutePeriodMetricsRow
import com.illa.cashvan.sales.feature.kpi.data.model.VisitDayRow
import com.illa.cashvan.shared.feature.kpi.data.model.AgentKpiResponse

/** The Sales KPI pages, leaderboard and commission, all read from Supabase. */
interface SalesKpiRepository {
    /**
     * `agent-kpi-mobile` for the presell agent; no month means the current Cairo month.
     * @throws KpiUnavailableException with the HTTP status (404 unknown agent, 409 not fully linked).
     */
    suspend fun getAgentKpi(month: Int? = null, year: Int? = null): AgentKpiResponse

    /** Every agent's month summary, unfiltered; coverage reads the agent's own row from it. */
    suspend fun getAgentsSummary(month: Int, year: Int): List<KpiAgentSummaryRow>

    /** [getAgentsSummary] restricted to active sales agents, as the leaderboard ranks them. */
    suspend fun getLeaderboardSummary(month: Int, year: Int): List<KpiAgentSummaryRow>

    suspend fun getRoutePeriodMetrics(startIso: String, endIso: String, routeId: String? = null): RoutePeriodMetricsRow?

    /** The agent's presell and cash van visits in [startIso, endIso). */
    suspend fun getVisits(startIso: String, endIso: String): List<VisitDayRow>

    /** The most frequent no-order reason in the period, in Arabic. */
    suspend fun getTopNoOrderReason(startIso: String, endIso: String): String?

    /** This month's live commission rows; null when the commission tables aren't migrated yet. */
    suspend fun getCommissionRows(monthStart: String): List<CommissionPayoutRow>?

    suspend fun getAssignedRoutes(): List<AssignedRouteRef>

    /** The signed-in presell agent's id, to find "me" in all-agent lists. */
    suspend fun currentAgentId(): String
}

class KpiUnavailableException(val status: Int, cause: Throwable) : Exception(cause.message, cause) {
    /** The PWA's messages; 404 and 409 aren't worth retrying. */
    val userMessage: String
        get() = when (status) {
            404 -> "المؤشرات غير متاحة. لم يتم العثور على المندوب، تواصل مع المشرف."
            409 -> "المؤشرات غير متاحة. حسابك غير مربوط بالكامل، تواصل مع الإدارة."
            else -> "المؤشرات غير متاحة الآن. حدّث وحاول مرة أخرى."
        }
}
