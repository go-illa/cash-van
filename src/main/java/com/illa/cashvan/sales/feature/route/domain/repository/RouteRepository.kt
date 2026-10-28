package com.illa.cashvan.sales.feature.route.domain.repository

import com.illa.cashvan.sales.domain.model.RouteMerchant
import com.illa.cashvan.sales.domain.route.RecentRemoteOrder
import com.illa.cashvan.sales.feature.route.data.model.AdHocMerchant
import com.illa.cashvan.sales.feature.route.data.model.RouteRef

interface RouteRepository {
    /** The agent's monthly journey plan. Empty when no route is assigned. */
    suspend fun getRouteMerchants(): List<RouteMerchant>

    /** Routes of the agent's that each merchant is on, keyed by merchant id. */
    suspend fun getRouteOptionsByMerchant(): Map<String, List<RouteRef>>

    /** Up to 20 of [merchantIds] whose name, sign name or code contains [query]. */
    suspend fun searchMerchants(merchantIds: Collection<String>, query: String): List<AdHocMerchant>

    /** Latest live telesales or retail-app order per merchant, keyed by merchant id. */
    suspend fun getRecentRemoteOrders(merchantIds: List<String>): Map<String, RecentRemoteOrder>
}
