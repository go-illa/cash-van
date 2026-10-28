package com.illa.cashvan.sales.feature.visit.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NewVisitRow(
    val agent_id: String,
    val merchant_id: String,
    val visit_date: String,
    /** Recorded as with_order and switched to without_order when the agent gives a no-order reason. */
    val visit_type: String,
    val latitude: Double,
    val longitude: Double,
    val location: String,
    val is_ad_hoc: Boolean
)

@Serializable
data class VisitIdRow(
    val id: String
)

@Serializable
data class NoOrderUpdate(
    val visit_type: String,
    val no_order_reason_id: String
)

@Serializable
data class NoOrderReason(
    val id: String,
    val reason_ar: String? = null,
    val reason_en: String? = null,
    val display_order: Int? = null
)

@Serializable
data class HistoryVisitRow(
    val id: String,
    val visit_date: String,
    val visit_type: String,
    val no_order_reason_id: String? = null
)

@Serializable
data class HistoryOrderRow(
    val id: String,
    val visit_id: String? = null,
    val total_value: Double? = null,
    val total_items: Int? = null,
    val payment_type: String? = null
)

@Serializable
data class HistoryOrderItemRow(
    val sales_order_id: String,
    val quantity: Int = 0,
    val unit_price: Double? = null,
    val product_id: String? = null,
    val products: ProductName? = null
)

@Serializable
data class ProductName(
    val name: String? = null
)

data class VisitHistoryItem(
    val visitId: String,
    val visitDate: String,
    val withOrder: Boolean,
    val noOrderReason: String?,
    val orders: List<VisitHistoryOrder>
)

data class VisitHistoryOrder(
    val orderId: String,
    val totalValue: Double,
    val totalItems: Int,
    val paymentType: String,
    val items: List<VisitHistoryOrderItem>
)

data class VisitHistoryOrderItem(
    val productName: String?,
    val quantity: Int,
    val unitPrice: Double?
)
