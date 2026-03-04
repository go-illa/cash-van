package com.illa.cashvan.feature.merchant.domain.repository

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.merchant.data.model.CreateMerchantRequest
import com.illa.cashvan.feature.merchant.data.model.CreateMerchantResponse
import com.illa.cashvan.feature.merchant.data.model.GovernoratesResponse
import com.illa.cashvan.feature.merchant.data.model.MerchantTypesResponse
import com.illa.cashvan.feature.merchant.data.model.ReverseGeocodeResponse

interface MerchantRepository {
    suspend fun createMerchant(request: CreateMerchantRequest): ApiResult<CreateMerchantResponse>
    suspend fun getGovernorates(): ApiResult<GovernoratesResponse>
    suspend fun getMerchantTypes(): ApiResult<MerchantTypesResponse>
    suspend fun reverseGeocode(latitude: String, longitude: String): ApiResult<ReverseGeocodeResponse>
}
