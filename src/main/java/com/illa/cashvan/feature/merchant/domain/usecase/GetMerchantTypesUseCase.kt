package com.illa.cashvan.feature.merchant.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.merchant.data.model.MerchantTypesResponse
import com.illa.cashvan.feature.merchant.domain.repository.MerchantRepository

class GetMerchantTypesUseCase(
    private val merchantRepository: MerchantRepository
) {
    suspend fun invoke(): ApiResult<MerchantTypesResponse> {
        return merchantRepository.getMerchantTypes()
    }
}
