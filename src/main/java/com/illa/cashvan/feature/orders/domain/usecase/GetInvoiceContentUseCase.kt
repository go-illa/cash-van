package com.illa.cashvan.feature.orders.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class GetInvoiceContentUseCase {
    suspend operator fun invoke(invoiceUrl: String): ApiResult<String> {
        return try {
            val plainClient = HttpClient(Android) {
                engine {
                    connectTimeout = 30_000
                    socketTimeout = 30_000
                }
            }

            val content = plainClient.get(invoiceUrl).bodyAsText()
            plainClient.close()

            ApiResult.Success(content)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to fetch invoice content")
        }
    }
}
