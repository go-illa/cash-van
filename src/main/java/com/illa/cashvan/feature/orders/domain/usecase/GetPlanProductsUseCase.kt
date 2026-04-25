package com.illa.cashvan.feature.orders.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.PlanProductsResponse
import com.illa.cashvan.feature.orders.domain.repository.OrderRepository

class GetPlanProductsUseCase(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(planId: String, query: String? = null, priceTier: String? = null, page: Int = 1, items: Int = 20): ApiResult<PlanProductsResponse> {
        return orderRepository.getPlanProducts(planId, query, priceTier, page, items)
    }
}