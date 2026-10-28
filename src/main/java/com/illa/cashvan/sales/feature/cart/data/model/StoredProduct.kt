package com.illa.cashvan.sales.feature.cart.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A product as it was when added to the cart.
 *
 * @property price the final price minus the cash discount.
 * @property price_info the `product_prices` row behind [price], sent as the order line's price snapshot.
 */
@Serializable
data class StoredProduct(
    val id: String,
    val description: String,
    val sku: String,
    val price: Double,
    val principal_id: String? = null,
    val image_url: String? = null,
    val price_info: JsonObject? = null
)

data class StoredCartLine(
    val product: StoredProduct,
    val quantity: Int,
    val principalName: String
)

data class StoredRequestLine(
    val product: StoredProduct,
    val quantity: Int
)
