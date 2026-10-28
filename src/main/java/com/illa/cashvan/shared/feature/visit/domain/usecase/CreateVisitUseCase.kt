package com.illa.cashvan.shared.feature.visit.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.shared.feature.visit.data.model.CreateVisitRequest
import com.illa.cashvan.shared.feature.visit.data.model.CreateVisitResponse
import com.illa.cashvan.shared.feature.visit.domain.repository.VisitRepository

class CreateVisitUseCase(private val visitRepository: VisitRepository) {
    suspend operator fun invoke(request: CreateVisitRequest): ApiResult<CreateVisitResponse> =
        visitRepository.createVisit(request)
}
