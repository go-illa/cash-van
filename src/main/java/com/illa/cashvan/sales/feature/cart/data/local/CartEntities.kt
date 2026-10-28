package com.illa.cashvan.sales.feature.cart.data.local

import androidx.room.Entity

/**
 * One cart line. Carts are scoped to an agent and a merchant, like the PWA's
 * `sales_agent_cart::<agent>::<merchant>` storage key.
 *
 * @property productJson the product with its price row at the time it was added, as sent in the order's
 * price snapshot.
 */
@Entity(
    tableName = "presell_cart_items",
    primaryKeys = ["agentId", "merchantId", "productId"]
)
data class CartItemEntity(
    val agentId: String,
    val merchantId: String,
    val productId: String,
    val quantity: Int,
    val principalName: String,
    val productJson: String,
    val addedAt: Long
)

/**
 * A deferred out-of-stock request line ("طلب مؤجل", DIS-446). These carry no price and create no order.
 */
@Entity(
    tableName = "presell_request_items",
    primaryKeys = ["agentId", "merchantId", "productId"]
)
data class RequestItemEntity(
    val agentId: String,
    val merchantId: String,
    val productId: String,
    val quantity: Int,
    val productJson: String,
    val addedAt: Long
)

/**
 * Idempotency keys that live as long as their basket, so a retried submit can't create a second order or
 * request. Each is replaced when its basket is cleared.
 */
@Entity(
    tableName = "presell_cart_meta",
    primaryKeys = ["agentId", "merchantId"]
)
data class CartMetaEntity(
    val agentId: String,
    val merchantId: String,
    val idempotencyKey: String,
    val requestId: String
)
