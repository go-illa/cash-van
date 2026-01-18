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
    val status: String? = null,
    val order_type: String? = null,
    val cancellation_reason: String? = null,
    val cancellation_note: String? = null,
    val locus_code: String? = null,
    val order_plan_products: List<OrderPlanProduct>? = null,
    val merchant: Merchant? = null,
    val invoice_attachment: InvoiceAttachment? = null
)

@Serializable
data class InvoiceAttachment(
    val id: String,
    val name: String,
    val document_type: String,
    val document_type_name: String,
    val url: String,
    val thumbnail_url: String? = null,
    val large_url: String? = null,
    val file_size: String,
    val content_type: String,
    val filename: String,
    val created_at: String,
    val updated_at: String
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
    val status: String? = null,
    val product: OrderProduct? = null,
    val plan_product_price: PlanProductPrice? = null
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
    val description: String,
    val vat: Double? = null,
    val tax_code: String? = null
)

@Serializable
data class PlanProductPrice(
    val id: String,
    val created_at: String,
    val updated_at: String,
    val plan_product_id: String,
    val price_tier: String? = null,
    val discounted: Boolean = false,
    val active: Boolean = true,
    val vat_percentage: Double? = null,
    val tax_percentage: Double? = null,
    val vat_application: String? = null,
    val discount_type: String? = null,
    val discount_details: DiscountDetails? = null,
    val start_date: String? = null,
    val end_date: String? = null,
    val final_price: String,
    val base_price: String,
    val cash_discount: String? = null,
    val plan_type: String? = null,
    val price_details: PriceDetails? = null
)

@Serializable
data class DiscountDetails(
    val base_price: String? = null,
    val discount_type: String? = null,
    val discount_value: String? = null
)

@Serializable
data class PriceDetails(
    val final_price: Double? = null,
    val tax_amount: Double? = null,
    val vat_amount: Double? = null,
    val discount_amount: Double? = null
)

@Serializable
data class Merchant(
    val id: String,
    val created_at: String? = null,
    val updated_at: String? = null,
    val name: String? = null,
    val sign_name: String? = null,
    val phone_number: String? = null,
    val address: String? = null,
    val google_link: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val governorate_id: String? = null,
    val creator_id: String? = null,
    val creator_type: String? = null,
    val code: String? = null,
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
    val assigned_quantity: Int? = 0,
    val sold_quantity: Int? = 0,
    val product_price: String,
    val total_income: String,
    val total_price: String,
    val cash_van_available_quantity: Int? = 0,
    val plan_id: String? = null,
    val product_id: String? = null,
    val product: Product
) {
    val calculatedAvailableQuantity: Int
        get() = cash_van_available_quantity ?: ((assigned_quantity ?: 0) - (sold_quantity ?: 0)).coerceAtLeast(0)
}

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

@Serializable
data class UpdateOrderRequest(
    val order: UpdateOrderData
)

@Serializable
data class UpdateOrderData(
    val status: String? = null,
    val cancellation_reason: String? = null,
    val cancellation_note: String? = null,
    val order_items: List<SubmitOrderItem>? = null
)

@Serializable
data class SubmitOrderItem(
    val plan_product_id: String,
    val sold_quantity: Int
)