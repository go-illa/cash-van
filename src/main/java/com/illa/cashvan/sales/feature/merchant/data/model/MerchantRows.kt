package com.illa.cashvan.sales.feature.merchant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PresellMerchantType(
    val id: String,
    val name: String
)

@Serializable
data class RegionRow(
    val id: String,
    val name: String? = null,
    val arabic_name: String? = null,
    val english_name: String? = null
)

@Serializable
data class AgentRouteOption(
    val route_id: String,
    val routes: RouteName? = null
)

@Serializable
data class RouteName(
    val name: String? = null
)

/**
 * A new merchant always starts on the retail tier; any other tier is sent as a pending tier request.
 */
@Serializable
data class NewMerchantRow(
    val name: String,
    val sign_name: String,
    val merchant_type_id: String,
    val phone_number: String,
    val secondary_phone: String?,
    val governorate: String,
    val region_id: String?,
    val route_id: String?,
    val detailed_address: String,
    val location: String,
    val latitude: Double?,
    val longitude: Double?,
    val created_by_agent_id: String,
    val created_by_agent_name: String?,
    val created_by_agent_code: String?,
    val price_tier: String,
    val requested_price_tier: String?,
    val tier_request_status: String?,
    val tier_requested_at: String? = null
)

@Serializable
data class MerchantIdRow(
    val id: String
)

@Serializable
data class MerchantVisitDayRow(
    val merchant_id: String,
    val day_of_week: String
)

@Serializable
data class MerchantRouteLink(
    val merchant_id: String,
    val route_id: String
)

data class NewMerchantForm(
    val merchantName: String,
    val signName: String,
    val merchantTypeId: String,
    val phoneNumber: String,
    val secondaryPhone: String?,
    val governorate: String,
    val regionId: String,
    val detailedAddress: String,
    val priceTier: String,
    val latitude: Double?,
    val longitude: Double?,
    /** `sunday` … `saturday`. */
    val visitDays: List<String>,
    /** Required when the agent has more than one route. */
    val routeId: String?
)
