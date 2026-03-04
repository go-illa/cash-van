package com.illa.cashvan.feature.merchant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateMerchantRequest(
    val merchant: MerchantData
)

@Serializable
data class MerchantData(
    val name: String,
    val sign_name: String,
    val phone_number: String,
    val secondary_phone: String? = null,
    val merchant_type_id: String,
    val price_tier: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val plan_id: Int,
    val working_days: Map<String, Boolean>
)
