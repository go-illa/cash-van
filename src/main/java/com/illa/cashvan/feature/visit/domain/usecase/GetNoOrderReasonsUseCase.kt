package com.illa.cashvan.feature.visit.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.visit.data.model.NoOrderReasonsResponse
import com.illa.cashvan.feature.visit.domain.repository.VisitRepository

class GetNoOrderReasonsUseCase(private val visitRepository: VisitRepository) {
    suspend operator fun invoke(): ApiResult<NoOrderReasonsResponse> =
        visitRepository.getNoOrderReasons()
}
