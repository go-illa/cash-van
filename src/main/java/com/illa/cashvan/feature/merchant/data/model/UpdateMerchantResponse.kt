package com.illa.cashvan.feature.merchant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateMerchantResponse(
    val id: String,
    val name: String,
    val sign_name: String? = null,
    val address: String? = null,
    val phone_number: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val code: String? = null,
    val price_tier: String? = null,
    val plan_id: String? = null,
    val governorate_id: String? = null,
    val creator_id: String? = null,
    val creator_type: String? = null,
    val google_link: String? = null
)
