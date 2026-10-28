package com.illa.cashvan.sales.feature.cart.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CartItemEntity::class, RequestItemEntity::class, CartMetaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SalesDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao

    companion object {
        const val NAME = "sales.db"
    }
}
