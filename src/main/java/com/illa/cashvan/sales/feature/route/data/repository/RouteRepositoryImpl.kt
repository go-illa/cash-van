package com.illa.cashvan.sales.feature.route.data.repository

import com.illa.cashvan.core.supabase.SupabaseClient
import com.illa.cashvan.core.supabase.selectAll
import com.illa.cashvan.core.supabase.selectInChunks
import com.illa.cashvan.sales.data.CurrentAgentProvider
import com.illa.cashvan.sales.data.startOfMonthIso
import com.illa.cashvan.sales.domain.model.RouteMerchant
import com.illa.cashvan.sales.domain.route.RecentRemoteOrder
import com.illa.cashvan.sales.domain.route.RemoteOrderChannel
import com.illa.cashvan.sales.feature.route.data.model.AdHocMerchant
import com.illa.cashvan.sales.feature.route.data.model.AgentRouteRow
import com.illa.cashvan.sales.feature.route.data.model.MerchantRouteOptionRow
import com.illa.cashvan.sales.feature.route.data.model.RouteRef
import com.illa.cashvan.sales.feature.route.data.model.MerchantRouteRow
import com.illa.cashvan.sales.feature.route.data.model.MonthlyVisitRow
import com.illa.cashvan.sales.feature.route.data.model.RemoteOrderRow
import com.illa.cashvan.sales.feature.route.data.model.RouteMerchantRow
import com.illa.cashvan.sales.feature.route.domain.repository.RouteRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class RouteRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val currentAgentProvider: CurrentAgentProvider
) : RouteRepository {

    override suspend fun getRouteMerchants(): List<RouteMerchant> = coroutineScope {
        val agent = currentAgentProvider.require()

        val agentRoutes = supabaseClient.select<AgentRouteRow>("agent_routes") {
            select("route_id,routes(id,name)")
            eq("agent_id", agent.agentId)
        }
        if (agentRoutes.isEmpty()) return@coroutineScope emptyList()

        val routeNames = agentRoutes.mapNotNull { row -> row.routes?.name?.let { row.route_id to it } }.toMap()
        val merchantRoutes = supabaseClient.selectAll<MerchantRouteRow>("merchant_routes") {
            select("merchant_id,route_id")
            isIn("route_id", agentRoutes.map { it.route_id })
        }
        if (merchantRoutes.isEmpty()) return@coroutineScope emptyList()

        val merchantIds = merchantRoutes.map { it.merchant_id }.distinct()
        val monthStart = startOfMonthIso()

        val merchants = async {
            supabaseClient.selectInChunks<RouteMerchantRow>("merchants", merchantIds) { chunk ->
                select(RouteMerchantRow.COLUMNS)
                isIn("id", chunk)
            }
        }
        val presellVisits = async {
            supabaseClient.selectInChunks<MonthlyVisitRow>("visits", merchantIds) { chunk ->
                select(VISIT_COLUMNS)
                eq("agent_id", agent.agentId)
                isIn("merchant_id", chunk)
                gte("visit_date", monthStart)
            }
        }
        // Visits made in the Cash Van flow are stored against the Rails sales agent, with no agent_id.
        val cashVanVisits = async {
            val salesAgentId = agent.salesAgentId ?: return@async emptyList()
            supabaseClient.selectInChunks<MonthlyVisitRow>("visits", merchantIds) { chunk ->
                select(VISIT_COLUMNS)
                isNull("agent_id")
                eq("sales_agent_id", salesAgentId)
                isIn("merchant_id", chunk)
                gte("visit_date", monthStart)
            }
        }

        assembleRouteMerchants(
            merchants = merchants.await(),
            merchantRoutes = merchantRoutes,
            routeNames = routeNames,
            visits = presellVisits.await() + cashVanVisits.await()
        )
    }

    override suspend fun getRouteOptionsByMerchant(): Map<String, List<RouteRef>> {
        val agent = currentAgentProvider.require()
        val routeIds = supabaseClient.select<AgentRouteRow>("agent_routes") {
            select("route_id")
            eq("agent_id", agent.agentId)
        }.map { it.route_id }
        if (routeIds.isEmpty()) return emptyMap()
        return supabaseClient.selectAll<MerchantRouteOptionRow>("merchant_routes") {
            select("merchant_id,route_id,routes(id,name)")
            isIn("route_id", routeIds)
        }
            .mapNotNull { row -> row.routes?.let { row.merchant_id to it } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, routes) -> routes.distinctBy { it.id } }
    }

    override suspend fun searchMerchants(merchantIds: Collection<String>, query: String): List<AdHocMerchant> {
        // Characters that would break the or() filter or act as wildcards are dropped or escaped, as in the PWA.
        val term = query.trim()
            .replace("%", "\\%").replace("_", "\\_")
            .replace(",", "").replace("(", "").replace(")", "")
        if (term.isEmpty()) return emptyList()
        return supabaseClient.selectInChunks<AdHocMerchant>("merchants", merchantIds.toList()) { chunk ->
            select("id,merchant_code,name,sign_name,location,price_tier,region_id")
            isIn("id", chunk)
            or("name.ilike.*$term*", "sign_name.ilike.*$term*", "merchant_code.ilike.*$term*")
            limit(AD_HOC_RESULT_LIMIT)
        }.distinctBy { it.id }.take(AD_HOC_RESULT_LIMIT)
    }

    override suspend fun getRecentRemoteOrders(merchantIds: List<String>): Map<String, RecentRemoteOrder> {
        if (merchantIds.isEmpty()) return emptyMap()
        val rows = supabaseClient.rpc<List<RemoteOrderRow>>(
            "get_recent_remote_orders",
            buildJsonObject {
                putJsonArray("p_merchant_ids") { merchantIds.distinct().sorted().forEach { add(it) } }
                // The badge lasts 3 calendar days; one extra rolling day keeps day-3 orders in the window.
                put("p_days", REMOTE_ORDER_WINDOW_DAYS + 1)
            }
        )
        // A channel this app can't label is ignored, like the PWA does.
        return rows.mapNotNull { row ->
            RemoteOrderChannel.fromWire(row.channel)?.let { row.merchant_id to RecentRemoteOrder(row.last_order_at, it) }
        }.toMap()
    }

    private companion object {
        const val VISIT_COLUMNS = "merchant_id,visit_date,visit_type,is_ad_hoc"
        const val AD_HOC_RESULT_LIMIT = 20
        const val REMOTE_ORDER_WINDOW_DAYS = 3
    }
}
