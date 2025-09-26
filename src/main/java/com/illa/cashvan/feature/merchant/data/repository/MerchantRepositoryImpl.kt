package com.illa.cashvan.feature.merchant.data.repository

import com.illa.cashvan.core.network.endpoint.ApiEndpoints
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.merchant.data.model.CreateMerchantRequest
import com.illa.cashvan.feature.merchant.data.model.CreateMerchantResponse
import com.illa.cashvan.feature.merchant.domain.repository.MerchantRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType

class MerchantRepositoryImpl(
    private val httpClient: HttpClient
) : MerchantRepository {

    override suspend fun createMerchant(request: CreateMerchantRequest): ApiResult<CreateMerchantResponse> {
        return try {
            val config = ApiEndpoints.Merchant.createMerchant()
            val versionedPath = "v${config.version}/${config.path}"

            val response = httpClient.request(versionedPath) {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<CreateMerchantResponse>()

            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to create merchant")
        }
    }
}