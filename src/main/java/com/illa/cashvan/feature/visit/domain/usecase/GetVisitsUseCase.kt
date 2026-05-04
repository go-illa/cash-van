package com.illa.cashvan.feature.visit.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.visit.data.model.VisitsListResponse
import com.illa.cashvan.feature.visit.domain.repository.VisitRepository

class GetVisitsUseCase(private val visitRepository: VisitRepository) {
    suspend operator fun invoke(page: Int = 1, items: Int = 20): ApiResult<VisitsListResponse> =
        visitRepository.getVisits(page, items)
}
