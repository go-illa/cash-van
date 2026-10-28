package com.illa.cashvan.sales.feature.orders.domain.repository

import com.illa.cashvan.sales.feature.orders.data.model.PresellHistoryOrder

interface PresellOrderRepository {
    /** The agent's presell orders in the current Cairo calendar month, newest first. */
    suspend fun getMonthOrders(): List<PresellHistoryOrder>

    /** @throws CancelOrderException with the RPC's code, e.g. `GRACE_PERIOD_EXPIRED`. */
    suspend fun cancelOrder(orderId: String)
}

class CancelOrderException(val code: String) : Exception(code)
