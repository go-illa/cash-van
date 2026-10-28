package com.illa.cashvan.shared.feature.visit.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.shared.feature.visit.data.model.VisitItem
import com.illa.cashvan.shared.feature.visit.domain.repository.VisitRepository

class GetVisitByIdUseCase(private val visitRepository: VisitRepository) {
    suspend operator fun invoke(id: String): ApiResult<VisitItem> =
        visitRepository.getVisitById(id)
}
