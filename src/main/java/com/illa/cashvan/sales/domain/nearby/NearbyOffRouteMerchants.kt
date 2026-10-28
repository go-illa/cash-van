package com.illa.cashvan.sales.domain.nearby

import com.illa.cashvan.sales.domain.cashvan.jsStringToNumber
import com.illa.cashvan.sales.domain.format.jsToFixed
import com.illa.cashvan.sales.domain.route.calculateDistance
import kotlin.math.PI
import kotlin.math.cos

// DIS-316 — discover nearby merchants that are NOT on the agent's route. Pure
// parts of the PWA's src/hooks/useNearbyOffRouteMerchants.ts: query key,
// bounding box, Haversine refinement, route classification and
// reassignment-request status. The Supabase queries are not ported.

/** Discovery radius in meters (ticket spec: 1 km). */
const val NEARBY_RADIUS_METERS = 1000.0

/** Pad so the bounding box comfortably contains the Haversine circle. */
const val NEARBY_BOX_PAD = 1.1

/** Bounding-box query row cap. */
const val NEARBY_QUERY_LIMIT = 500

private const val METERS_PER_DEGREE_LATITUDE = 111_320.0
private const val MIN_LONGITUDE_COSINE = 0.0001

data class GeoPoint(val latitude: Double, val longitude: Double)

enum class MerchantClassification(val wireValue: String) {
    UNASSIGNED("unassigned"),
    OTHER_ROUTE("other_route"),
    MY_ROUTE("my_route"),
}

/**
 * Cache identity of a nearby search: coordinates rounded to 5 decimals (~1 m)
 * so GPS jitter does not trigger reloads, and the route IDs sorted and joined.
 */
data class NearbyQueryKey(val latitude: Double?, val longitude: Double?, val routeIds: String)

/**
 * Refresh policy: nearby data loads once and never refreshes on its own.
 * [staleTimeMs] = [Long.MAX_VALUE] is react-query's `Infinity`; a null interval is `false`.
 */
data class NearbyQueryRefreshOptions(
    val staleTimeMs: Long,
    val refetchIntervalMs: Long?,
    val refetchOnMount: Boolean,
    val refetchOnWindowFocus: Boolean,
    val refetchOnReconnect: Boolean,
)

/** Merchant row from the bounding-box query. Coordinates are the raw values (numbers as their JSON text). */
data class RawNearbyMerchant(
    val id: String,
    val name: String,
    val signName: String?,
    val merchantCode: String?,
    val latitude: String?,
    val longitude: String?,
    val location: String?,
    val phoneNumber: String?,
)

/** A raw merchant within the radius, with its parsed coordinates and distance in meters. */
data class NearbyCandidate(
    val merchant: RawNearbyMerchant,
    val latitude: Double?,
    val longitude: Double?,
    val distance: Double,
)

data class NearbyBoundingBox(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double,
)

data class MerchantRouteRow(val merchantId: String, val routeId: String)

data class RouteNameRow(val id: String, val name: String)

/** `merchant_route_reassignment_requests` row; the list must be ordered by requested_at DESC. */
data class ReassignmentRequestRow(
    val id: String,
    val merchantId: String,
    val status: String,
    val declineNote: String?,
    val requestedAt: String?,
    val requestType: String?,
)

data class NearbyMerchant(
    val id: String,
    /** Sign name, else name. */
    val name: String,
    val signName: String?,
    val merchantCode: String?,
    val latitude: Double?,
    val longitude: Double?,
    val location: String?,
    val phoneNumber: String?,
    /** Distance from the agent in meters. */
    val distance: Double,
    /** All routes this merchant is linked to (via merchant_routes). */
    val routeIds: List<String>,
    /** Route names, falling back to the route ID. */
    val routeNames: List<String>,
    val classification: MerchantClassification,
    /** Most recent pending WhatsApp request (pending guard). */
    val pendingRequestId: String?,
    /** Decline note of the merchant's most recent request when it is declined ("" without a note). */
    val lastDeclineNote: String?,
    /** True when the most recent request is declined (blocks re-requesting). */
    val lastDeclined: Boolean,
)

/** Request status of one merchant, from its requests newest first. */
data class ReassignmentRequestStatus(
    val pendingRequestId: String?,
    val lastDeclineNote: String?,
    val lastDeclined: Boolean,
)

private val NO_REQUEST_STATUS = ReassignmentRequestStatus(pendingRequestId = null, lastDeclineNote = null, lastDeclined = false)

fun getNearbyOffRouteMerchantsQueryKey(location: GeoPoint?, routeIds: List<String>): NearbyQueryKey =
    NearbyQueryKey(
        latitude = location?.let { jsStringToNumber(jsToFixed(it.latitude, 5)) },
        longitude = location?.let { jsStringToNumber(jsToFixed(it.longitude, 5)) },
        routeIds = routeIds.sorted().joinToString(","),
    )

fun getNearbyQueryRefreshOptions(): NearbyQueryRefreshOptions = NearbyQueryRefreshOptions(
    staleTimeMs = Long.MAX_VALUE,
    refetchIntervalMs = null,
    refetchOnMount = false,
    refetchOnWindowFocus = false,
    refetchOnReconnect = false,
)

/** Box around the agent for the coarse query; longitude widens with 1/cos(latitude). */
fun nearbyBoundingBox(latitude: Double, longitude: Double): NearbyBoundingBox {
    val dLat = NEARBY_RADIUS_METERS * NEARBY_BOX_PAD / METERS_PER_DEGREE_LATITUDE
    val dLng = dLat / maxOf(cos(latitude * PI / 180), MIN_LONGITUDE_COSINE)
    return NearbyBoundingBox(
        minLatitude = latitude - dLat,
        maxLatitude = latitude + dLat,
        minLongitude = longitude - dLng,
        maxLongitude = longitude + dLng,
    )
}

/** JS `Number(v)` for a raw coordinate: null/"" → null, unparseable → null (Infinity passes through). */
fun parseNearbyCoordinate(value: String?): Double? {
    if (value.isNullOrEmpty()) return null
    return jsStringToNumber(value).takeIf { !it.isNaN() }
}

/** Exact Haversine refinement: merchants within [NEARBY_RADIUS_METERS], nearest first (stable). */
fun refineNearbyMerchants(raw: List<RawNearbyMerchant>, latitude: Double, longitude: Double): List<NearbyCandidate> =
    raw.map { merchant ->
        val mLat = parseNearbyCoordinate(merchant.latitude)
        val mLng = parseNearbyCoordinate(merchant.longitude)
        val distance = if (mLat != null && mLng != null) {
            calculateDistance(latitude, longitude, mLat, mLng)
        } else {
            Double.POSITIVE_INFINITY
        }
        NearbyCandidate(merchant, mLat, mLng, distance)
    }
        .filter { it.distance <= NEARBY_RADIUS_METERS }
        .sortedBy { it.distance }

/** Classification uses ONLY merchant_routes; merchants.route_id is no longer maintained. */
fun classifyNearbyMerchant(merchantRouteIds: List<String>, myRouteIds: Set<String>): MerchantClassification = when {
    merchantRouteIds.isEmpty() -> MerchantClassification.UNASSIGNED
    merchantRouteIds.any { it in myRouteIds } -> MerchantClassification.MY_ROUTE
    else -> MerchantClassification.OTHER_ROUTE
}

/**
 * Per-merchant request status from rows ordered newest first. The first
 * pending WhatsApp request is the pending guard; a decline note surfaces only
 * when the merchant's newest request is the declined one (a newer
 * pending/approved request supersedes an older decline).
 */
fun resolveReassignmentRequestStatuses(requests: List<ReassignmentRequestRow>): Map<String, ReassignmentRequestStatus> {
    val pendingByMerchant = HashMap<String, String>()
    val lastDeclineByMerchant = HashMap<String, String>()
    val newestRequestSeen = HashSet<String>()

    for (row in requests) {
        val isNewestForMerchant = newestRequestSeen.add(row.merchantId)
        if (row.status == "pending" && row.requestType == "whatsapp" && row.merchantId !in pendingByMerchant) {
            pendingByMerchant[row.merchantId] = row.id
        }
        if (row.status == "declined" && isNewestForMerchant) {
            lastDeclineByMerchant[row.merchantId] = row.declineNote ?: ""
        }
    }

    return newestRequestSeen.associateWith { merchantId ->
        ReassignmentRequestStatus(
            pendingRequestId = pendingByMerchant[merchantId],
            lastDeclineNote = lastDeclineByMerchant[merchantId],
            lastDeclined = merchantId in lastDeclineByMerchant,
        )
    }
}

/** Route IDs referenced by the merchant_routes rows, in first-seen order (for the route-names query). */
fun involvedRouteIds(merchantRoutes: List<MerchantRouteRow>): List<String> = merchantRoutes.map { it.routeId }.distinct()

/**
 * Final list in [within] order (nearest first), joining route membership,
 * route names and request status.
 */
fun assembleNearbyMerchants(
    within: List<NearbyCandidate>,
    merchantRoutes: List<MerchantRouteRow>,
    routes: List<RouteNameRow>,
    requests: List<ReassignmentRequestRow>,
    myRouteIds: List<String>,
): List<NearbyMerchant> {
    val routesByMerchant = merchantRoutes.groupBy({ it.merchantId }, { it.routeId })
    // Later rows win on duplicate IDs, like `new Map(entries)`.
    val routeNames = routes.associate { it.id to it.name }
    val requestStatuses = resolveReassignmentRequestStatuses(requests)
    val myRouteSet = myRouteIds.toSet()

    return within.map { candidate ->
        val merchant = candidate.merchant
        val merchantRouteIds = routesByMerchant[merchant.id].orEmpty()
        val status = requestStatuses[merchant.id] ?: NO_REQUEST_STATUS
        NearbyMerchant(
            id = merchant.id,
            name = merchant.signName?.takeIf { it.isNotEmpty() } ?: merchant.name,
            signName = merchant.signName,
            merchantCode = merchant.merchantCode,
            latitude = candidate.latitude,
            longitude = candidate.longitude,
            location = merchant.location,
            phoneNumber = merchant.phoneNumber,
            distance = candidate.distance,
            routeIds = merchantRouteIds,
            routeNames = merchantRouteIds.map { routeNames[it] ?: it },
            classification = classifyNearbyMerchant(merchantRouteIds, myRouteSet),
            pendingRequestId = status.pendingRequestId,
            lastDeclineNote = status.lastDeclineNote,
            lastDeclined = status.lastDeclined,
        )
    }
}
