package com.illa.cashvan.sales.feature.nearby.data.repository

import com.illa.cashvan.core.supabase.SupabaseClient
import com.illa.cashvan.core.supabase.SupabaseException
import com.illa.cashvan.core.supabase.selectInChunks
import com.illa.cashvan.sales.data.CurrentAgentProvider
import com.illa.cashvan.sales.feature.nearby.data.model.MerchantRouteLinkRow
import com.illa.cashvan.sales.feature.nearby.data.model.NearbyMerchantRow
import com.illa.cashvan.sales.feature.nearby.data.model.NewReassignmentRequest
import com.illa.cashvan.sales.feature.nearby.data.model.ReassignmentRequestRow
import com.illa.cashvan.sales.feature.nearby.data.model.RouteMerchantLocationRow
import com.illa.cashvan.sales.feature.nearby.data.model.RouteNameRow
import com.illa.cashvan.sales.feature.nearby.data.model.SettingValueRow
import com.illa.cashvan.sales.feature.nearby.domain.repository.NearbyRepository
import com.illa.cashvan.sales.feature.nearby.domain.repository.PendingRequestExistsException

class NearbyRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val currentAgentProvider: CurrentAgentProvider
) : NearbyRepository {

    override suspend fun getMerchantsInBox(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): List<NearbyMerchantRow> =
        supabaseClient.select("merchants") {
            select("id,name,sign_name,merchant_code,latitude,longitude,location,phone_number")
            gte("latitude", minLat)
            lte("latitude", maxLat)
            gte("longitude", minLng)
            lte("longitude", maxLng)
            notNull("latitude")
            notNull("longitude")
            limit(BOX_LIMIT)
        }

    override suspend fun getMerchantRoutes(merchantIds: List<String>): List<MerchantRouteLinkRow> =
        supabaseClient.selectInChunks("merchant_routes", merchantIds) { chunk ->
            select("merchant_id,route_id")
            isIn("merchant_id", chunk)
        }

    override suspend fun getRouteNames(routeIds: Collection<String>): List<RouteNameRow> =
        supabaseClient.selectInChunks("routes", routeIds.distinct()) { chunk ->
            select("id,name")
            isIn("id", chunk)
        }

    override suspend fun getRequestsFor(merchantIds: List<String>): List<ReassignmentRequestRow> =
        supabaseClient.selectInChunks<ReassignmentRequestRow>("merchant_route_reassignment_requests", merchantIds) { chunk ->
            select("id,merchant_id,status,decline_note,requested_at,request_type")
            isIn("merchant_id", chunk)
            order("requested_at", ascending = false)
        }.sortedByDescending { it.requested_at.orEmpty() }

    override suspend fun getMyRequests(): List<ReassignmentRequestRow> {
        val agent = currentAgentProvider.require()
        return supabaseClient.select("merchant_route_reassignment_requests") {
            select("id,merchant_id,merchant_name,merchant_code,from_route_name,to_route_name,request_type,status,decline_note,requested_at,resolved_at")
            eq("agent_id", agent.agentId)
            order("requested_at", ascending = false)
            limit(HISTORY_LIMIT)
        }
    }

    override suspend fun getRouteMerchantLocations(routeIds: Collection<String>): List<RouteMerchantLocationRow> =
        if (routeIds.isEmpty()) emptyList()
        else supabaseClient.select("merchant_routes") {
            select("route_id,merchants!inner(latitude,longitude)")
            isIn("route_id", routeIds)
        }

    override suspend fun getWhatsAppNumber(): String? =
        supabaseClient.selectFirstOrNull<SettingValueRow>("settings") {
            select("value")
            eq("key", "reassignment_whatsapp_number")
        }?.value?.takeIf { it.isNotBlank() }

    override suspend fun linkMerchantToRoute(merchantId: String, routeId: String) {
        try {
            supabaseClient.insert<MerchantRouteLinkRow, MerchantRouteLinkRow>(
                "merchant_routes",
                MerchantRouteLinkRow(merchantId, routeId)
            ) { select("merchant_id,route_id") }
        } catch (e: SupabaseException) {
            if (e.code != UNIQUE_VIOLATION) throw e
        }
    }

    override suspend fun createRequest(request: NewReassignmentRequest) {
        try {
            supabaseClient.insert<NewReassignmentRequest, ReassignmentRequestRow>("merchant_route_reassignment_requests", request) {
                select("id")
            }
        } catch (e: SupabaseException) {
            // A unique index allows one pending request per merchant.
            if (e.code == UNIQUE_VIOLATION) throw PendingRequestExistsException(e)
            throw e
        }
    }

    private companion object {
        const val BOX_LIMIT = 500
        const val HISTORY_LIMIT = 50
        const val UNIQUE_VIOLATION = "23505"
    }
}
