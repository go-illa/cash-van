package com.illa.cashvan.feature.orders.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.MerchantSearchResponse
import com.illa.cashvan.feature.orders.domain.repository.OrderRepository

class SearchMerchantsUseCase(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(query: String, latitude: Double, longitude: Double, page: Int = 1, items: Int = 20): ApiResult<MerchantSearchResponse> {
        return orderRepository.searchMerchants(query, latitude, longitude, page, items)
    }
}
