package com.illa.cashvan.shared.feature.visit.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.shared.feature.visit.data.model.NoOrderReasonsResponse
import com.illa.cashvan.shared.feature.visit.domain.repository.VisitRepository

class GetNoOrderReasonsUseCase(private val visitRepository: VisitRepository) {
    suspend operator fun invoke(): ApiResult<NoOrderReasonsResponse> =
        visitRepository.getNoOrderReasons()
}
