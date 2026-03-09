package com.illa.cashvan.feature.merchant.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.merchant.data.model.ReverseGeocodeResponse
import com.illa.cashvan.feature.merchant.domain.repository.MerchantRepository

class GetReverseGeocodeUseCase(
    private val merchantRepository: MerchantRepository
) {
    suspend fun invoke(
        latitude: String,
        longitude: String
    ): ApiResult<ReverseGeocodeResponse> {
        return merchantRepository.reverseGeocode(latitude, longitude)
    }
}
