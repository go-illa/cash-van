package com.illa.cashvan.sales.feature.nearby.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NearbyMerchantRow(
    val id: String,
    val name: String? = null,
    val sign_name: String? = null,
    val merchant_code: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location: String? = null,
    val phone_number: String? = null
)

@Serializable
data class MerchantRouteLinkRow(
    val merchant_id: String,
    val route_id: String
)

@Serializable
data class RouteNameRow(
    val id: String,
    val name: String? = null
)

@Serializable
data class ReassignmentRequestRow(
    val id: String,
    val merchant_id: String? = null,
    val merchant_name: String? = null,
    val merchant_code: String? = null,
    val from_route_name: String? = null,
    val to_route_name: String? = null,
    val request_type: String? = null,
    val status: String? = null,
    val decline_note: String? = null,
    val requested_at: String? = null,
    val resolved_at: String? = null
)

@Serializable
data class RouteMerchantLocationRow(
    val route_id: String,
    val merchants: MerchantLocation? = null
)

@Serializable
data class MerchantLocation(
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class SettingValueRow(
    val value: String? = null
)

/** A row for `merchant_route_reassignment_requests`. */
@Serializable
data class NewReassignmentRequest(
    val merchant_id: String,
    val merchant_name: String?,
    val merchant_code: String?,
    val agent_id: String,
    val agent_name: String?,
    val agent_phone: String?,
    val from_route_id: String? = null,
    val from_route_name: String? = null,
    val to_route_id: String,
    val to_route_name: String?,
    /** `direct` (added to an unassigned merchant, approved at once) or `whatsapp` (pending review). */
    val request_type: String,
    val status: String,
    val resolved_at: String? = null,
    /** `presell` or `cashvan`, for analytics. */
    val flow: String
)
