package com.illa.cashvan.feature.orders.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.MerchantSearchResponse
import com.illa.cashvan.feature.orders.domain.repository.OrderRepository

class SearchMerchantsUseCase(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(query: String): ApiResult<MerchantSearchResponse> {
        return orderRepository.searchMerchants(query)
    }
}