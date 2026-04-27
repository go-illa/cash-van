package com.illa.cashvan.feature.plans.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val created_at: String,
    val updated_at: String,
    val sku: String,
    val frontdoor_code: String? = null,
    val price: String,
    val name: String? = null,
    val description: String? = null
)