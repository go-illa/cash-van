package com.illa.cashvan.feature.merchant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MerchantTypesResponse(
    val merchant_types: List<MerchantType>
)

@Serializable
data class MerchantType(
    val id: String,
    val name: String,
    val description: String,
    val created_at: String? = null,
    val updated_at: String? = null
)
