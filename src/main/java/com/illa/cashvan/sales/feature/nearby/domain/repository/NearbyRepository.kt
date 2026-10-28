package com.illa.cashvan.sales.feature.nearby.domain.repository

import com.illa.cashvan.sales.feature.nearby.data.model.MerchantRouteLinkRow
import com.illa.cashvan.sales.feature.nearby.data.model.NearbyMerchantRow
import com.illa.cashvan.sales.feature.nearby.data.model.NewReassignmentRequest
import com.illa.cashvan.sales.feature.nearby.data.model.ReassignmentRequestRow
import com.illa.cashvan.sales.feature.nearby.data.model.RouteMerchantLocationRow
import com.illa.cashvan.sales.feature.nearby.data.model.RouteNameRow

/** Merchants near the agent that are off their route, and the requests to move them onto it. */
interface NearbyRepository {
    /** Merchants with coordinates inside the box, at most 500. */
    suspend fun getMerchantsInBox(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): List<NearbyMerchantRow>

    suspend fun getMerchantRoutes(merchantIds: List<String>): List<MerchantRouteLinkRow>

    suspend fun getRouteNames(routeIds: Collection<String>): List<RouteNameRow>

    /** Requests for these merchants by anyone, newest first. */
    suspend fun getRequestsFor(merchantIds: List<String>): List<ReassignmentRequestRow>

    /** The agent's own requests, newest first. */
    suspend fun getMyRequests(): List<ReassignmentRequestRow>

    /** Merchant locations on the agent's routes, to preselect the nearest route. */
    suspend fun getRouteMerchantLocations(routeIds: Collection<String>): List<RouteMerchantLocationRow>

    /** `settings.reassignment_whatsapp_number`, or null when not configured. */
    suspend fun getWhatsAppNumber(): String?

    /** Links an unassigned merchant to the route; an existing link is fine. */
    suspend fun linkMerchantToRoute(merchantId: String, routeId: String)

    /** @throws PendingRequestExistsException when a pending request for the merchant already exists. */
    suspend fun createRequest(request: NewReassignmentRequest)
}

class PendingRequestExistsException(cause: Throwable) : Exception(cause.message, cause)
