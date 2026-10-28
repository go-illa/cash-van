package com.illa.cashvan.shared.feature.orders.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.shared.feature.orders.data.model.Order
import com.illa.cashvan.shared.feature.orders.data.model.UpdateOrderRequest
import com.illa.cashvan.shared.feature.orders.domain.repository.OrderRepository

class UpdateOrderUseCase(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(
        orderId: String,
        request: UpdateOrderRequest
    ): ApiResult<Order> {
        return orderRepository.updateOrder(orderId, request)
    }
}
