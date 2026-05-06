package com.illa.cashvan.feature.merchant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateMerchantRequest(val merchant: UpdateMerchantData)

@Serializable
data class UpdateMerchantData(val sign_name: String)
