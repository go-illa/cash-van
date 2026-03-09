package com.illa.cashvan.feature.merchant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NearestMerchantsResponse(
    val merchants: List<NearestMerchant>
)

@Serializable
data class NearestMerchant(
    val id: String,
    val name: String,
    val address: String? = null,
    val google_link: String? = null,
    val phone_number: String? = null,
    val latitude: String,
    val longitude: String,
    val governorate_id: String? = null,
    val code: String? = null,
    val sign_name: String? = null,
    val price_tier: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)
