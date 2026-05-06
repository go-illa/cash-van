package com.illa.cashvan.feature.merchant.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.merchant.data.model.UpdateMerchantResponse
import com.illa.cashvan.feature.merchant.domain.repository.MerchantRepository

class UpdateMerchantUseCase(private val merchantRepository: MerchantRepository) {
    suspend operator fun invoke(
        merchantId: String,
        signName: String,
        latitude: Double,
        longitude: Double
    ): ApiResult<UpdateMerchantResponse> =
        merchantRepository.updateMerchant(merchantId, signName, latitude, longitude)
}
