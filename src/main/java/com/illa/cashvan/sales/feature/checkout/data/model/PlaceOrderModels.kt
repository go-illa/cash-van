package com.illa.cashvan.sales.feature.checkout.data.model

import com.illa.cashvan.core.location.LocationData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

data class PlaceOrderRequest(
    val merchantId: String,
    val priceTier: String,
    /** `cash`, `credit` or `fawry`. */
    val paymentType: String,
    /** `yyyy-MM-dd`, credit orders only. */
    val collectionDate: String?,
    val isAdHoc: Boolean,
    val location: LocationData?,
    val fawryInvoiceUrl: String?,
    val totalValue: Double,
    val idempotencyKey: String,
    val regionId: String?,
    val lines: List<OrderLine>
)

data class OrderLine(
    val productId: String,
    val quantity: Int,
    val unitPrice: Double,
    /** The `product_prices` row the price came from. */
    val priceSnapshot: JsonObject?
)

@Serializable
data class InventoryShortage(
    val product_id: String,
    val sku: String,
    val requested: Double,
    val available: Double,
    val actual_available: Double? = null,
    val expected_available: Double? = null,
    val status: String,
    val warehouse_id: String? = null,
    val facility_name: String? = null
)

@Serializable
data class PlaceOrderResponse(
    val success: Boolean = false,
    val code: String? = null,
    val message: String? = null,
    val shortages: List<InventoryShortage> = emptyList(),
    val order_id: String? = null,
    val visit_id: String? = null,
    val order_total: Double? = null,
    val inventory_enforcement_bypassed: Boolean? = null,
    val inventory_reservation_source: String? = null
)

data class PlacedOrder(
    val orderId: String,
    val visitId: String,
    val orderTotal: Double,
    val inventoryReservationSource: String
)

@Serializable
data class FawryUploadRequest(
    val file_base64: String,
    val content_type: String,
    val path_id: String
)

@Serializable
data class FawryUploadResponse(
    val url: String
)
