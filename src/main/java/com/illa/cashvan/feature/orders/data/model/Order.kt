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
    val plan_id: Int? = null
)

@Serializable
data class OrdersResponse(
    val data: List<Order>
)

@Serializable
data class Plan(
    val id: Int,
    val created_at: String,
    val updated_at: String,
    val code: Int,
    val year: Int,
    val formatted_code: String,
    val infor_code: String,
    val status: String,
    val creator_id: Int,
    val creator_type: String,
    val sales_agent_id: Int
)

@Serializable
data class OngoingPlanResponse(
    val id: Int,
    val created_at: String,
    val updated_at: String,
    val code: Int,
    val year: Int,
    val formatted_code: String,
    val infor_code: String,
    val status: String,
    val creator_id: Int,
    val creator_type: String,
    val sales_agent_id: Int
)

@Serializable
data class MerchantSearchResponse(
    val merchants: List<MerchantItem>
)

@Serializable
data class MerchantItem(
    val id: Int,
    val created_at: String,
    val updated_at: String,
    val name: String,
    val address: String? = null,
    val google_link: String? = null,
    val phone_number: String,
    val latitude: String? = null,
    val longitude: String? = null,
    val governorate_id: Int? = null,
    val creator_id: Int,
    val creator_type: String
)

@Serializable
data class PlanProductsResponse(
    val plan_products: List<PlanProduct>
)

@Serializable
data class PlanProduct(
    val id: Int,
    val created_at: String,
    val updated_at: String,
    val assigned_quantity: Int,
    val sold_quantity: Int,
    val product_price: String,
    val total_income: String,
    val total_price: String,
    val available_quantity: Int,
    val plan_id: Int,
    val product_id: Int,
    val product: Product
)

@Serializable
data class Product(
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
data class CreateOrderRequest(
    val order: OrderData
)

@Serializable
data class OrderData(
    val plan_id: String,
    val merchant_id: String,
    val order_items: List<OrderItem>
)

@Serializable
data class OrderItem(
    val plan_product_id: String,
    val sold_quantity: String
)

@Serializable
data class CreateOrderResponse(
    val id: Int,
    val created_at: String,
    val updated_at: String,
    val plan_id: Int,
    val merchant_id: Int,
    val formatted_code: String,
    val creator_id: Int,
    val creator_type: String,
    val total_sold_quantity: Int,
    val total_income: String
)