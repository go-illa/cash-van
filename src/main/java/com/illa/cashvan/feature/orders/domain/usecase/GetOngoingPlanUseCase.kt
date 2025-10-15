package com.illa.cashvan.feature.orders.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.OngoingPlanResponse
import com.illa.cashvan.feature.orders.domain.repository.OrderRepository

class GetOngoingPlanUseCase(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(): ApiResult<OngoingPlanResponse?> {
        return orderRepository.getOngoingPlan()
    }
}