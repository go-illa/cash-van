package com.illa.cashvan.feature.merchant.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.merchant.data.model.CreateMerchantRequest
import com.illa.cashvan.feature.merchant.data.model.CreateMerchantResponse
import com.illa.cashvan.feature.merchant.data.model.MerchantData
import com.illa.cashvan.feature.merchant.domain.repository.MerchantRepository

class CreateMerchantUseCase(
    private val merchantRepository: MerchantRepository
) {
    suspend fun invoke(
        name: String,
        phoneNumber: String,
        latitude: String,
        longitude: String,
        planId: String
    ): ApiResult<CreateMerchantResponse> {
        val merchantData = MerchantData(
            name = name,
            phone_number = phoneNumber,
            latitude = latitude,
            longitude = longitude,
            plan_id = planId
        )

        val request = CreateMerchantRequest(merchant = merchantData)
        return merchantRepository.createMerchant(request)
    }
}