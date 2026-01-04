package com.illa.cashvan.feature.orders.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.Order
import com.illa.cashvan.feature.orders.data.model.UpdateOrderRequest
import com.illa.cashvan.feature.orders.domain.repository.OrderRepository
import javax.inject.Inject

class UpdateOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(
        orderId: String,
        request: UpdateOrderRequest
    ): ApiResult<Order> {
        return orderRepository.updateOrder(orderId, request)
    }
}
