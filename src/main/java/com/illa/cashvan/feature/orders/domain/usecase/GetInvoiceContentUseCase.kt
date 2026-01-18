package com.illa.cashvan.feature.orders.domain.usecase

import android.util.Log
import com.illa.cashvan.core.network.model.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class GetInvoiceContentUseCase {
    suspend operator fun invoke(invoiceUrl: String): ApiResult<String> {
        return try {
            Log.d("GetInvoiceContent", "Downloading invoice from URL: $invoiceUrl")

            // Create a plain HTTP client without auth headers for direct S3 download
            val plainClient = HttpClient(Android) {
                engine {
                    connectTimeout = 30_000
                    socketTimeout = 30_000
                }
            }

            val content = plainClient.get(invoiceUrl).bodyAsText()
            plainClient.close()

            Log.d("GetInvoiceContent", "Successfully downloaded invoice, size: ${content.length} bytes")
            ApiResult.Success(content)
        } catch (e: Exception) {
            Log.e("GetInvoiceContent", "Failed to download invoice", e)
            ApiResult.Error(e, e.message ?: "Failed to fetch invoice content")
        }
    }
}
