package com.illa.cashvan.feature.merchant.domain.repository

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.merchant.data.model.CreateMerchantRequest
import com.illa.cashvan.feature.merchant.data.model.CreateMerchantResponse

interface MerchantRepository {
    suspend fun createMerchant(request: CreateMerchantRequest): ApiResult<CreateMerchantResponse>
}