package com.illa.cashvan.feature.orders.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.Order
import com.illa.cashvan.feature.orders.domain.repository.OrderRepository

class GetOrderByIdUseCase(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(orderId: String): ApiResult<Order> {
        return orderRepository.getOrderById(orderId)
    }
}
