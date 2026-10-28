package com.illa.cashvan.sales.feature.cart.domain.repository

import com.illa.cashvan.sales.feature.cart.data.model.StoredCartLine
import com.illa.cashvan.sales.feature.cart.data.model.StoredProduct
import com.illa.cashvan.sales.feature.cart.data.model.StoredRequestLine
import kotlinx.coroutines.flow.Flow

/** The presell cart and deferred-request basket for one merchant, scoped to the signed-in agent. */
interface PresellCartRepository {
    fun observeCart(merchantId: String): Flow<List<StoredCartLine>>

    fun observeRequests(merchantId: String): Flow<List<StoredRequestLine>>

    /** Adds [quantity] to the line, creating it if needed. */
    suspend fun addToCart(merchantId: String, product: StoredProduct, quantity: Int, principalName: String)

    /** A quantity of zero or less removes the line. */
    suspend fun setCartQuantity(merchantId: String, productId: String, quantity: Int)

    /** Replaces products after a price refresh, keeping quantities. */
    suspend fun updateProducts(merchantId: String, products: List<StoredProduct>)

    /** Empties the cart and rotates its idempotency key. */
    suspend fun clearCart(merchantId: String)

    suspend fun idempotencyKey(merchantId: String): String

    /** A quantity of zero or less removes the line. */
    suspend fun setRequestQuantity(merchantId: String, product: StoredProduct, quantity: Int)

    /** Empties the deferred basket and rotates its request id. */
    suspend fun clearRequests(merchantId: String)

    suspend fun requestId(merchantId: String): String
}
