package com.illa.cashvan.feature.orders.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.VoidInvoiceResponse
import com.illa.cashvan.feature.orders.domain.repository.OrderRepository

class VoidInvoiceUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(orderId: String): ApiResult<VoidInvoiceResponse> =
        orderRepository.voidInvoice(orderId)
}
