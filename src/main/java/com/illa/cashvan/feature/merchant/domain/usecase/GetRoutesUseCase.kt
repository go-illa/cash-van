package com.illa.cashvan.feature.merchant.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.merchant.data.model.RoutesResponse
import com.illa.cashvan.feature.merchant.domain.repository.MerchantRepository

class GetRoutesUseCase(private val merchantRepository: MerchantRepository) {
    suspend operator fun invoke(): ApiResult<RoutesResponse> = merchantRepository.getRoutes()
}
