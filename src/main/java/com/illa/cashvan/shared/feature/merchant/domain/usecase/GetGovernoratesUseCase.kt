package com.illa.cashvan.shared.feature.merchant.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.shared.feature.merchant.data.model.GovernoratesResponse
import com.illa.cashvan.shared.feature.merchant.domain.repository.MerchantRepository

class GetGovernoratesUseCase(
    private val merchantRepository: MerchantRepository
) {
    suspend fun invoke(): ApiResult<GovernoratesResponse> {
        return merchantRepository.getGovernorates()
    }
}
