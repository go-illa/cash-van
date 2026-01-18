package com.illa.cashvan.feature.plans.data.model

import kotlinx.serialization.Serializable



@Serializable
data class PlanProduct(
    val id: String,
    val created_at: String,
    val updated_at: String,
    val assigned_quantity: Int,
    val sold_quantity: Int,
    val product_price: String,
    val plan_id: String,
    val product_id: String,
    val product: Product,
    val pre_sell_available_quantity: Int = 0,
    val cash_van_available_quantity: Int = 0
)