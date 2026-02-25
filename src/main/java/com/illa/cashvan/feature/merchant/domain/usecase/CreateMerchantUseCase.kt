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
        signName: String,
        phoneNumber: String,
        secondaryPhoneNumber: String?,
        latitude: Double,
        longitude: Double,
        planId: Int,
        merchantTypeId: String,
        detailedAddress: String,
        priceTier: String,
        workingDays: Map<String, Boolean>
    ): ApiResult<CreateMerchantResponse> {
        val merchantData = MerchantData(
            name = name,
            sign_name = signName,
            phone_number = phoneNumber,
            secondary_phone = secondaryPhoneNumber,
            merchant_type_id = merchantTypeId,
            price_tier = priceTier,
            detailed_address = detailedAddress,
            latitude = latitude,
            longitude = longitude,
            plan_id = planId,
            working_days = workingDays
        )

        val request = CreateMerchantRequest(merchant = merchantData)
        return merchantRepository.createMerchant(request)
    }
}
