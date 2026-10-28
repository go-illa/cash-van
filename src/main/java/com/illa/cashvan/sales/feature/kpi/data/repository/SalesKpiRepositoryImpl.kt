package com.illa.cashvan.sales.feature.kpi.data.repository

import com.illa.cashvan.core.supabase.SupabaseClient
import com.illa.cashvan.core.supabase.SupabaseException
import com.illa.cashvan.core.supabase.selectAll
import com.illa.cashvan.sales.data.CurrentAgentProvider
import com.illa.cashvan.sales.feature.kpi.data.model.AgentIdRow
import com.illa.cashvan.sales.feature.kpi.data.model.AgentKpiRequestBody
import com.illa.cashvan.sales.feature.kpi.data.model.AssignedRouteRef
import com.illa.cashvan.sales.feature.kpi.data.model.AssignedRouteRow
import com.illa.cashvan.sales.feature.kpi.data.model.CommissionPayoutRow
import com.illa.cashvan.sales.feature.kpi.data.model.KpiAgentSummaryRow
import com.illa.cashvan.sales.feature.kpi.data.model.NoOrderReasonIdRow
import com.illa.cashvan.sales.feature.kpi.data.model.ReasonTextRow
import com.illa.cashvan.sales.feature.kpi.data.model.RoutePeriodMetricsRow
import com.illa.cashvan.sales.feature.kpi.data.model.VisitDayRow
import com.illa.cashvan.sales.feature.kpi.domain.repository.KpiUnavailableException
import com.illa.cashvan.sales.feature.kpi.domain.repository.SalesKpiRepository
import com.illa.cashvan.shared.feature.kpi.data.model.AgentKpiResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.Collator
import java.util.Locale

class SalesKpiRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val currentAgentProvider: CurrentAgentProvider
) : SalesKpiRepository {

    override suspend fun getAgentKpi(month: Int?, year: Int?): AgentKpiResponse {
        val agent = currentAgentProvider.require()
        return try {
            supabaseClient.invokeFunction<AgentKpiRequestBody, AgentKpiResponse>(
                "agent-kpi-mobile",
                AgentKpiRequestBody(agent_id = agent.agentId, agent_type = PRESELL, month = month, year = year)
            )
        } catch (e: SupabaseException) {
            throw KpiUnavailableException(e.status, e)
        }
    }

    override suspend fun getAgentsSummary(month: Int, year: Int): List<KpiAgentSummaryRow> =
        supabaseClient.rpc(
            "get_kpi_agents_summary",
            buildJsonObject {
                put("p_month", month)
                put("p_year", year)
            }
        )

    override suspend fun getLeaderboardSummary(month: Int, year: Int): List<KpiAgentSummaryRow> = coroutineScope {
        val rows = async { getAgentsSummary(month, year) }
        val eligible = async {
            supabaseClient.selectAll<AgentIdRow>("agents") {
                select("id")
                eq("agent_type", "sales_agent")
                eq("status", 1)
            }.map { it.id }.toSet()
        }
        val allowed = eligible.await()
        rows.await().filter { it.agent_id in allowed }
    }

    override suspend fun getRoutePeriodMetrics(startIso: String, endIso: String, routeId: String?): RoutePeriodMetricsRow? {
        val agent = currentAgentProvider.require()
        return supabaseClient.rpc<List<RoutePeriodMetricsRow>>(
            "get_agent_route_period_metrics",
            buildJsonObject {
                put("p_agent_id", agent.agentId)
                put("p_start_at", startIso)
                put("p_end_at", endIso)
                if (routeId != null) put("p_route_id", routeId) else put("p_route_id", JsonNull)
            }
        ).firstOrNull()
    }

    override suspend fun getVisits(startIso: String, endIso: String): List<VisitDayRow> = coroutineScope {
        val agent = currentAgentProvider.require()
        val presell = async {
            supabaseClient.selectAll<VisitDayRow>("visits") {
                select("visit_date,is_ad_hoc")
                eq("agent_id", agent.agentId)
                gte("visit_date", startIso)
                lt("visit_date", endIso)
            }
        }
        val cashVan = async {
            val salesAgentId = agent.salesAgentId ?: return@async emptyList<VisitDayRow>()
            supabaseClient.selectAll<VisitDayRow>("visits") {
                select("visit_date,is_ad_hoc")
                isNull("agent_id")
                eq("sales_agent_id", salesAgentId)
                gte("visit_date", startIso)
                lt("visit_date", endIso)
            }
        }
        presell.await() + cashVan.await()
    }

    override suspend fun getTopNoOrderReason(startIso: String, endIso: String): String? = coroutineScope {
        val agent = currentAgentProvider.require()
        val presell = async {
            supabaseClient.selectAll<NoOrderReasonIdRow>("visits") {
                select("no_order_reason_id")
                eq("agent_id", agent.agentId)
                eq("visit_type", "without_order")
                notNull("no_order_reason_id")
                gte("visit_date", startIso)
                lt("visit_date", endIso)
            }
        }
        val cashVan = async {
            val salesAgentId = agent.salesAgentId ?: return@async emptyList<NoOrderReasonIdRow>()
            supabaseClient.selectAll<NoOrderReasonIdRow>("visits") {
                select("no_order_reason_id")
                isNull("agent_id")
                eq("sales_agent_id", salesAgentId)
                eq("visit_type", "without_order")
                notNull("no_order_reason_id")
                gte("visit_date", startIso)
                lt("visit_date", endIso)
            }
        }
        val top = (presell.await() + cashVan.await()).mapNotNull { it.no_order_reason_id }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: return@coroutineScope null
        supabaseClient.selectFirstOrNull<ReasonTextRow>("no_order_reasons") {
            select("reason_ar")
            eq("id", top)
        }?.reason_ar
    }

    override suspend fun getCommissionRows(monthStart: String): List<CommissionPayoutRow>? {
        val agent = currentAgentProvider.require()
        return try {
            supabaseClient.select("commission_payouts") {
                select("target_egp,actual_egp,payout_egp,payout_at_threshold_egp,min_threshold_pct,snapshotted_at")
                eq("agent_id", agent.agentId)
                eq("is_live", true)
                eq("month", monthStart)
                limit(COMMISSION_ROW_LIMIT)
            }
        } catch (e: SupabaseException) {
            // A missing column or table means commissions aren't rolled out here; the card hides.
            if (e.code in NOT_MIGRATED) null else throw e
        }
    }

    override suspend fun getAssignedRoutes(): List<AssignedRouteRef> {
        val agent = currentAgentProvider.require()
        val arabic = Collator.getInstance(Locale("ar"))
        return supabaseClient.select<AssignedRouteRow>("agent_routes") {
            select("route_id,routes(id,name,color)")
            eq("agent_id", agent.agentId)
        }.mapNotNull { it.routes }.distinctBy { it.id }.sortedWith(compareBy(arabic) { it.name.orEmpty() })
    }

    override suspend fun currentAgentId(): String = currentAgentProvider.require().agentId

    private companion object {
        const val PRESELL = "presell"
        const val COMMISSION_ROW_LIMIT = 10
        val NOT_MIGRATED = setOf("42703", "42P01")
    }
}
