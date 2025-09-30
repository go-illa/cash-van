package com.illa.cashvan.feature.orders.domain.repository

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.Order
import com.illa.cashvan.feature.orders.data.model.OrdersResponse

interface OrderRepository {
    suspend fun getOrders(createdAtDateEq: String? = null): ApiResult<OrdersResponse>
    suspend fun getOrderById(orderId: String): ApiResult<Order>
}