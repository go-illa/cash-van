package com.illa.cashvan.feature.orders.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String,
    val created_at: String,
    val updated_at: String,
    val plan_id: String? = null,
    val formatted_code: String,
    val creator_id: String? = null,
    val creator_type: String? = null,
    val total_sold_quantity: Int,
    val total_income: String,
    val order_plan_products: List<OrderPlanProduct>? = null,
    val merchant: Merchant? = null
)

@Serializable
data class OrderPlanProduct(
    val id: String,
    val created_at: String,
    val updated_at: String,
    val order_id: String? = null,
    val plan_product_id: String? = null,
    val sold_quantity: Int,
    val total_income: String,
    val product: OrderProduct? = null
)

@Serializable
data class OrderProduct(
    val id: String,
    val created_at: String,
    val updated_at: String,
    val sku: String,
    val frontdoor_code: String,
    val price: String,
    val name: String,
    val description: String
)

@Serializable
data class Merchant(
    val id: String,
    val created_at: String?=null,
    val updated_at: String?=null,
    val name: String?=null,
    val phone_number: String?=null,
    val address: String? = null,
    val google_link: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val governorate_id: String? = null,
    val creator_id: String? = null,
    val creator_type: String? = null,
    val plan_id: String? = null
)

@Serializable
data class OrdersResponse(
    val data: List<Order>
)

@Serializable
data class OngoingPlanResponse(
    val id: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val code: Int? = null,
    val year: Int? = null,
    val formatted_code: String? = null,
    val infor_code: String? = null,
    val status: String? = null,
    val creator_id: String? = null,
    val creator_type: String? = null,
    val sales_agent_id: String? = null
)

@Serializable
data class MerchantSearchResponse(
    val merchants: List<MerchantItem>
)

@Serializable
data class MerchantItem(
    val id: String,
    val created_at: String? = null,
    val updated_at: String? = null,
    val name: String,
    val address: String? = null,
    val google_link: String? = null,
    val phone_number: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val governorate_id: String? = null,
    val creator_id: String? = null,
    val creator_type: String? = null
)

@Serializable
data class PlanProductsResponse(
    val plan_products: List<PlanProduct>
)

@Serializable
data class PlanProduct(
    val id: String,
    val created_at: String,
    val updated_at: String,
    val assigned_quantity: Int,
    val sold_quantity: Int,
    val product_price: String,
    val total_income: String,
    val total_price: String,
    val available_quantity: Int,
    val plan_id: String? = null,
    val product_id: String? = null,
    val product: Product
)

@Serializable
data class Product(
    val id: String,
    val created_at: String,
    val updated_at: String,
    val sku: String,
    val frontdoor_code: String,
    val price: String,
    val name: String,
    val description: String
)

@Serializable
data class CreateOrderRequest(
    val order: OrderData
)

@Serializable
data class OrderData(
    val plan_id: String? = null,
    val merchant_id: String? = null,
    val order_items: List<OrderItem>
)

@Serializable
data class OrderItem(
    val plan_product_id: String? = null,
    val sold_quantity: String
)

@Serializable
data class CreateOrderResponse(
    val id: String,
    val created_at: String,
    val updated_at: String,
    val plan_id: String? = null,
    val merchant_id: String? = null,
    val formatted_code: String,
    val creator_id: String? = null,
    val creator_type: String? = null,
    val total_sold_quantity: Int,
    val total_income: String
)