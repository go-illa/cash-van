package com.illa.cashvan.feature.orders.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.OrdersResponse
import com.illa.cashvan.feature.orders.domain.repository.OrderRepository
import javax.inject.Inject

class GetOrdersUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val getOngoingPlanUseCase: GetOngoingPlanUseCase
) {
    suspend operator fun invoke(createdAtDateEq: String? = null): ApiResult<OrdersResponse> {
        // First get the ongoing plan to retrieve the plan_id
        val planResult = getOngoingPlanUseCase()
        val planId = when (planResult) {
            is ApiResult.Success -> planResult.data?.id
            else -> null
        }

        return orderRepository.getOrders(planId, createdAtDateEq)
    }
}