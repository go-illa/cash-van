package com.illa.cashvan.feature.merchant.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.merchant.data.model.NearestMerchantsResponse
import com.illa.cashvan.feature.merchant.domain.repository.MerchantRepository

class GetNearestMerchantsUseCase(
    private val merchantRepository: MerchantRepository
) {
    suspend fun invoke(
        latitude: String,
        longitude: String,
        radiusMeters: Int = 500
    ): ApiResult<NearestMerchantsResponse> {
        return merchantRepository.getNearestMerchants(latitude, longitude, radiusMeters)
    }
}
