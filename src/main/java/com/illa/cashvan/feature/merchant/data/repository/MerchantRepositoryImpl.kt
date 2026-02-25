package com.illa.cashvan.feature.merchant.data.repository

import com.illa.cashvan.core.network.endpoint.ApiEndpoints
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.merchant.data.model.CreateMerchantRequest
import com.illa.cashvan.feature.merchant.data.model.CreateMerchantResponse
import com.illa.cashvan.feature.merchant.data.model.GovernoratesResponse
import com.illa.cashvan.feature.merchant.data.model.MerchantTypesResponse
import com.illa.cashvan.feature.merchant.data.model.NearestMerchantsResponse
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

    override suspend fun getGovernorates(): ApiResult<GovernoratesResponse> {
        return try {
            val config = ApiEndpoints.Merchant.getGovernorates()
            val versionedPath = "v${config.version}/${config.path}"

            val response = httpClient.request(versionedPath) {
                method = HttpMethod.Get
            }.body<GovernoratesResponse>()

            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to get governorates")
        }
    }

    override suspend fun getMerchantTypes(): ApiResult<MerchantTypesResponse> {
        return try {
            val config = ApiEndpoints.Merchant.getMerchantTypes()
            val versionedPath = "v${config.version}/${config.path}"

            val response = httpClient.request(versionedPath) {
                method = HttpMethod.Get
            }.body<MerchantTypesResponse>()

            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to get merchant types")
        }
    }

    override suspend fun getNearestMerchants(
        latitude: String,
        longitude: String,
        radiusMeters: Int
    ): ApiResult<NearestMerchantsResponse> {
        return try {
            val config = ApiEndpoints.Merchant.getNearestMerchants(latitude, longitude, radiusMeters)
            val versionedPath = "v${config.version}/${config.path}"

            val response = httpClient.request(versionedPath) {
                method = HttpMethod.Get
                url {
                    parameters.append("latitude", latitude)
                    parameters.append("longitude", longitude)
                    parameters.append("radius_meters", radiusMeters.toString())
                }
            }.body<NearestMerchantsResponse>()

            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to get nearest merchants")
        }
    }
}
