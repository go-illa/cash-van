package com.illa.cashvan.feature.plans.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.plans.data.model.PlanProductsResponse
import com.illa.cashvan.feature.plans.domain.repository.PlansRepository

class GetPlanProductsUseCase(
    private val plansRepository: PlansRepository
) {
    suspend operator fun invoke(planId: String): ApiResult<PlanProductsResponse> {
        return plansRepository.getPlanProducts(planId)
    }
}