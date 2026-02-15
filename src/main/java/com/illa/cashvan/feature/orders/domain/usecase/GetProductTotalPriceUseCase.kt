package com.illa.cashvan.feature.orders.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.ProductPriceCalculationResponse
import com.illa.cashvan.feature.orders.domain.repository.OrderRepository
import javax.inject.Inject

class GetProductTotalPriceUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(
        planId: String,
        productId: String,
        orderId: String,
        quantity: Int
    ): ApiResult<ProductPriceCalculationResponse> {
        return orderRepository.getProductTotalPrice(planId, productId, orderId, quantity)
    }
}
