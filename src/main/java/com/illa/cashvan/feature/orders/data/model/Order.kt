package com.illa.cashvan.feature.orders.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: Int,
    val created_at: String,
    val updated_at: String,
    val plan_id: Int,
    val formatted_code: String,
    val creator_id: Int,
    val creator_type: String,
    val total_sold_quantity: Int,
    val total_income: String,
    val order_plan_products: List<OrderPlanProduct>? = null,
    val merchant: Merchant? = null
)

@Serializable
data class OrderPlanProduct(
    val id: Int,
    val created_at: String,
    val updated_at: String,
    val order_id: Int,
    val plan_product_id: Int,
    val sold_quantity: Int,
    val total_income: String,
    val product: OrderProduct? = null
)

@Serializable
data class OrderProduct(
    val id: Int,
    val created_at: String,
    val updated_at: String,
    val sku_code: String,
    val fd_sku_code: String,
    val price: String,
    val name: String,
    val description: String
)

@Serializable
data class Merchant(
    val id: Int,
    val created_at: String,
    val updated_at: String,
    val name: String,
    val phone_number: String,
    val address: String? = null,
    val google_link: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val governorate_id: Int? = null,
    val creator_id: Int,
    val creator_type: String,
    val plan_id: Int
)

@Serializable
data class OrdersResponse(
    val data: List<Order>
)