package com.illa.cashvan.feature.merchant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateMerchantResponse(
    val id: String,
    val name: String,
    val phone_number: String,
    val latitude: String,
    val longitude: String,
    val created_at: String? = null,
    val updated_at: String? = null
)