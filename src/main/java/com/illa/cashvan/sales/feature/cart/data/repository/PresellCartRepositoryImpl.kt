package com.illa.cashvan.sales.feature.cart.data.repository

import com.illa.cashvan.sales.data.CurrentAgentProvider
import com.illa.cashvan.sales.feature.cart.data.local.CartDao
import com.illa.cashvan.sales.feature.cart.data.local.CartItemEntity
import com.illa.cashvan.sales.feature.cart.data.local.CartMetaEntity
import com.illa.cashvan.sales.feature.cart.data.local.RequestItemEntity
import com.illa.cashvan.sales.feature.cart.data.model.StoredCartLine
import com.illa.cashvan.sales.feature.cart.data.model.StoredProduct
import com.illa.cashvan.sales.feature.cart.data.model.StoredRequestLine
import com.illa.cashvan.sales.feature.cart.domain.repository.PresellCartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID

class PresellCartRepositoryImpl(
    private val cartDao: CartDao,
    private val currentAgentProvider: CurrentAgentProvider
) : PresellCartRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeCart(merchantId: String): Flow<List<StoredCartLine>> = flow {
        val agentId = agentId()
        emitAll(cartDao.observeItems(agentId, merchantId).map { rows ->
            rows.mapNotNull { row -> decode(row.productJson)?.let { StoredCartLine(it, row.quantity, row.principalName) } }
        })
    }

    override fun observeRequests(merchantId: String): Flow<List<StoredRequestLine>> = flow {
        val agentId = agentId()
        emitAll(cartDao.observeRequestItems(agentId, merchantId).map { rows ->
            rows.mapNotNull { row -> decode(row.productJson)?.let { StoredRequestLine(it, row.quantity) } }
        })
    }

    override suspend fun addToCart(merchantId: String, product: StoredProduct, quantity: Int, principalName: String) {
        val agentId = agentId()
        val existing = cartDao.getItem(agentId, merchantId, product.id)
        cartDao.upsertItem(
            CartItemEntity(
                agentId = agentId,
                merchantId = merchantId,
                productId = product.id,
                quantity = (existing?.quantity ?: 0) + quantity,
                principalName = existing?.principalName ?: principalName,
                productJson = existing?.productJson ?: encode(product),
                addedAt = existing?.addedAt ?: System.currentTimeMillis()
            )
        )
    }

    override suspend fun setCartQuantity(merchantId: String, productId: String, quantity: Int) {
        val agentId = agentId()
        if (quantity <= 0) {
            cartDao.deleteItem(agentId, merchantId, productId)
            return
        }
        val existing = cartDao.getItem(agentId, merchantId, productId) ?: return
        cartDao.upsertItem(existing.copy(quantity = quantity))
    }

    override suspend fun updateProducts(merchantId: String, products: List<StoredProduct>) {
        val agentId = agentId()
        val byId = products.associateBy { it.id }
        val updated = cartDao.getItems(agentId, merchantId).mapNotNull { row ->
            byId[row.productId]?.let { row.copy(productJson = encode(it)) }
        }
        cartDao.upsertItems(updated)
    }

    override suspend fun clearCart(merchantId: String) {
        val agentId = agentId()
        cartDao.deleteItems(agentId, merchantId)
        val meta = meta(agentId, merchantId)
        cartDao.upsertMeta(meta.copy(idempotencyKey = UUID.randomUUID().toString()))
    }

    override suspend fun idempotencyKey(merchantId: String): String = meta(agentId(), merchantId).idempotencyKey

    override suspend fun setRequestQuantity(merchantId: String, product: StoredProduct, quantity: Int) {
        val agentId = agentId()
        if (quantity <= 0) {
            cartDao.deleteRequestItem(agentId, merchantId, product.id)
            return
        }
        val existing = cartDao.getRequestItem(agentId, merchantId, product.id)
        cartDao.upsertRequestItem(
            RequestItemEntity(
                agentId = agentId,
                merchantId = merchantId,
                productId = product.id,
                quantity = quantity,
                productJson = existing?.productJson ?: encode(product),
                addedAt = existing?.addedAt ?: System.currentTimeMillis()
            )
        )
    }

    override suspend fun clearRequests(merchantId: String) {
        val agentId = agentId()
        cartDao.deleteRequestItems(agentId, merchantId)
        val meta = meta(agentId, merchantId)
        cartDao.upsertMeta(meta.copy(requestId = UUID.randomUUID().toString()))
    }

    override suspend fun requestId(merchantId: String): String = meta(agentId(), merchantId).requestId

    private suspend fun meta(agentId: String, merchantId: String): CartMetaEntity =
        cartDao.getMeta(agentId, merchantId) ?: CartMetaEntity(
            agentId = agentId,
            merchantId = merchantId,
            idempotencyKey = UUID.randomUUID().toString(),
            requestId = UUID.randomUUID().toString()
        ).also { cartDao.upsertMeta(it) }

    private suspend fun agentId(): String = currentAgentProvider.require().agentId

    private fun encode(product: StoredProduct): String = json.encodeToString(StoredProduct.serializer(), product)

    private fun decode(value: String): StoredProduct? =
        runCatching { json.decodeFromString(StoredProduct.serializer(), value) }.getOrNull()
}
