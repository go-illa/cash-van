package com.illa.cashvan.sales.feature.route.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AgentRouteRow(
    val route_id: String,
    val routes: RouteRef? = null
)

@Serializable
data class RouteRef(
    val id: String,
    val name: String? = null
)

@Serializable
data class MerchantRouteRow(
    val merchant_id: String,
    val route_id: String
)

@Serializable
data class RouteMerchantRow(
    val id: String,
    val merchant_code: String? = null,
    val name: String? = null,
    val sign_name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location: String? = null,
    val price_tier: String? = null,
    val phone_number: String? = null,
    val secondary_phone: String? = null,
    val detailed_address: String? = null,
    val governorate: String? = null,
    val frontdoor_code: String? = null,
    val google_link: String? = null,
    val working_days: JsonObject? = null,
    val region_id: String? = null,
    val fawry_device_number: String? = null,
    val wants_fawry_device: Boolean? = null
) {
    companion object {
        const val COLUMNS = "id,merchant_code,name,sign_name,latitude,longitude,location,price_tier," +
            "phone_number,secondary_phone,detailed_address,governorate,frontdoor_code,google_link," +
            "working_days,region_id,fawry_device_number,wants_fawry_device"
    }
}

@Serializable
data class MonthlyVisitRow(
    val merchant_id: String,
    val visit_date: String,
    val visit_type: String,
    val is_ad_hoc: Boolean? = null
)

@Serializable
data class MerchantRouteOptionRow(
    val merchant_id: String,
    val route_id: String,
    val routes: RouteRef? = null
)

@Serializable
data class AdHocMerchant(
    val id: String,
    val merchant_code: String? = null,
    val name: String? = null,
    val sign_name: String? = null,
    val location: String? = null,
    val price_tier: String? = null,
    val region_id: String? = null
) {
    val displayName: String get() = sign_name?.takeIf { it.isNotBlank() } ?: name.orEmpty()
}

@Serializable
data class RemoteOrderRow(
    val merchant_id: String,
    val last_order_at: String,
    val channel: String
)
