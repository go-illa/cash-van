package com.illa.cashvan.shared.feature.plans.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.shared.feature.plans.data.model.PlansResponse
import com.illa.cashvan.shared.feature.plans.domain.repository.PlansRepository

class GetPlansUseCase(
    private val plansRepository: PlansRepository
) {
    suspend operator fun invoke(): ApiResult<PlansResponse> {
        return plansRepository.getPlans()
    }
}