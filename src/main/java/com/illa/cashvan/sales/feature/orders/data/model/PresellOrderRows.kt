package com.illa.cashvan.sales.feature.orders.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PresellHistoryOrder(
    val id: String,
    val order_date: String,
    val status: String,
    val payment_type: String? = null,
    val total_items: Int? = null,
    val total_value: Double? = null,
    val location: String? = null,
    val cancellation_reason: String? = null,
    val merchant: PresellHistoryMerchant? = null,
    val sales_order_items: List<PresellHistoryItem> = emptyList()
) {
    companion object {
        /** Same shape as the PWA's `ORDER_HISTORY_SELECT`. */
        const val SELECT = "id,order_date,status,payment_type,total_items,total_value,location,cancellation_reason," +
            "merchant:merchants(name,merchant_code,location,phone_number,secondary_phone,phone_invalid)," +
            "sales_order_items(id,quantity,unit_price,product:products(sku,description,price))"
    }
}

@Serializable
data class PresellHistoryMerchant(
    val name: String? = null,
    val merchant_code: String? = null,
    val location: String? = null,
    val phone_number: String? = null,
    val secondary_phone: String? = null,
    val phone_invalid: Boolean? = null
)

@Serializable
data class PresellHistoryItem(
    val id: String,
    val quantity: Int = 0,
    val unit_price: Double? = null,
    val product: PresellHistoryProduct? = null
)

@Serializable
data class PresellHistoryProduct(
    val sku: String? = null,
    val description: String? = null,
    val price: Double? = null
)

@Serializable
data class CancelOrderResult(
    val success: Boolean = false,
    val code: String? = null
)
