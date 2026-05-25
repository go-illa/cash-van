package com.illa.cashvan.feature.orders.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class GetInvoiceContentUseCase(
    private val plainHttpClient: HttpClient
) {
    suspend operator fun invoke(invoiceUrl: String): ApiResult<String> {
        return try {
            val content = plainHttpClient.get(invoiceUrl).bodyAsText()
            ApiResult.Success(content)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to fetch invoice content")
        }
    }
}
