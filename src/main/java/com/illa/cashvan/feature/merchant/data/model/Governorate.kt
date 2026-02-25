package com.illa.cashvan.feature.merchant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GovernoratesResponse(
    val governorates: List<Governorate>
)

@Serializable
data class Governorate(
    val id: String,
    val english_name: String,
    val arabic_name: String,
    val iso_code: String,
    val country_id: String,
    val created_at: String? = null,
    val updated_at: String? = null
)
