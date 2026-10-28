package com.illa.cashvan.sales.feature.cashvan.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NearbyMerchantsResponse(
    val merchants: List<NearbyMerchant> = emptyList()
)

@Serializable
data class NearbyMerchant(
    val id: String,
    val name: String? = null,
    val sign_name: String? = null,
    val code: String? = null,
    val phone_number: String? = null,
    val address: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val governorate_id: String? = null,
    val governorate: NamedRef? = null,
    val price_tier: String? = null,
    val google_link: String? = null
) {
    val displayName: String get() = sign_name?.takeIf { it.isNotBlank() } ?: name.orEmpty()
    val lat: Double? get() = latitude?.toDoubleOrNull()
    val lng: Double? get() = longitude?.toDoubleOrNull()
}

@Serializable
data class NamedRef(
    val name: String? = null
)

/** `GET merchants/:id`; Rails may wrap it in `{ merchant }`. */
@Serializable
data class RailsMerchantPricing(
    val id: String? = null,
    val price_tier: String? = null,
    val governorate_id: String? = null
)

@Serializable
data class RailsMerchantEnvelope(
    val merchant: RailsMerchantPricing? = null,
    val id: String? = null,
    val price_tier: String? = null,
    val governorate_id: String? = null
) {
    fun unwrap(): RailsMerchantPricing = merchant ?: RailsMerchantPricing(id, price_tier, governorate_id)
}

@Serializable
data class FawryBadgeRow(
    val id: String,
    val fawry_device_number: String? = null,
    val wants_fawry_device: Boolean? = null
)

/** A merchant the agent picked for a cash van order. Tier and governorate are re-read from Rails. */
data class CashVanMerchantContext(
    val merchantId: String,
    val merchantName: String
)
