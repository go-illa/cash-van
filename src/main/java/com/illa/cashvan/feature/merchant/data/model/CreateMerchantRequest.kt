package com.illa.cashvan.feature.merchant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateMerchantRequest(
    val merchant: MerchantData
)

@Serializable
data class MerchantData(
    val name: String,
    val phone_number: String,
    val latitude: String,
    val longitude: String,
    val plan_id: String
)