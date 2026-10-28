package com.illa.cashvan.sales.feature.cart.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM presell_cart_items WHERE agentId = :agentId AND merchantId = :merchantId ORDER BY addedAt")
    fun observeItems(agentId: String, merchantId: String): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM presell_cart_items WHERE agentId = :agentId AND merchantId = :merchantId ORDER BY addedAt")
    suspend fun getItems(agentId: String, merchantId: String): List<CartItemEntity>

    @Query("SELECT * FROM presell_cart_items WHERE agentId = :agentId AND merchantId = :merchantId AND productId = :productId")
    suspend fun getItem(agentId: String, merchantId: String, productId: String): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: CartItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<CartItemEntity>)

    @Query("DELETE FROM presell_cart_items WHERE agentId = :agentId AND merchantId = :merchantId AND productId = :productId")
    suspend fun deleteItem(agentId: String, merchantId: String, productId: String)

    @Query("DELETE FROM presell_cart_items WHERE agentId = :agentId AND merchantId = :merchantId")
    suspend fun deleteItems(agentId: String, merchantId: String)

    @Query("SELECT * FROM presell_request_items WHERE agentId = :agentId AND merchantId = :merchantId ORDER BY addedAt")
    fun observeRequestItems(agentId: String, merchantId: String): Flow<List<RequestItemEntity>>

    @Query("SELECT * FROM presell_request_items WHERE agentId = :agentId AND merchantId = :merchantId AND productId = :productId")
    suspend fun getRequestItem(agentId: String, merchantId: String, productId: String): RequestItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRequestItem(item: RequestItemEntity)

    @Query("DELETE FROM presell_request_items WHERE agentId = :agentId AND merchantId = :merchantId AND productId = :productId")
    suspend fun deleteRequestItem(agentId: String, merchantId: String, productId: String)

    @Query("DELETE FROM presell_request_items WHERE agentId = :agentId AND merchantId = :merchantId")
    suspend fun deleteRequestItems(agentId: String, merchantId: String)

    @Query("SELECT * FROM presell_cart_meta WHERE agentId = :agentId AND merchantId = :merchantId")
    suspend fun getMeta(agentId: String, merchantId: String): CartMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: CartMetaEntity)
}
