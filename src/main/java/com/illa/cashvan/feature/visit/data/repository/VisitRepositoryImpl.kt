package com.illa.cashvan.feature.visit.data.repository

import com.illa.cashvan.core.network.endpoint.ApiEndpoints
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.visit.data.model.CreateVisitRequest
import com.illa.cashvan.feature.visit.data.model.CreateVisitResponse
import com.illa.cashvan.feature.visit.data.model.NoOrderReasonsResponse
import com.illa.cashvan.feature.visit.data.model.VisitItem
import com.illa.cashvan.feature.visit.data.model.VisitsListResponse
import com.illa.cashvan.feature.visit.domain.repository.VisitRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType

class VisitRepositoryImpl(
    private val httpClient: HttpClient
) : VisitRepository {

    override suspend fun getNoOrderReasons(): ApiResult<NoOrderReasonsResponse> {
        return try {
            val config = ApiEndpoints.Visits.getNoOrderReasons()
            val versionedPath = "v${config.version}/${config.path}"
            val response = httpClient.request(versionedPath) {
                method = HttpMethod.Get
            }.body<NoOrderReasonsResponse>()
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to fetch no-order reasons")
        }
    }

    override suspend fun createVisit(request: CreateVisitRequest): ApiResult<CreateVisitResponse> {
        return try {
            val config = ApiEndpoints.Visits.createVisit()
            val versionedPath = "v${config.version}/${config.path}"
            val response = httpClient.request(versionedPath) {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<CreateVisitResponse>()
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to create visit")
        }
    }

    override suspend fun getVisits(page: Int, items: Int): ApiResult<VisitsListResponse> {
        return try {
            val config = ApiEndpoints.Visits.getVisits(page, items)
            val versionedPath = "v${config.version}/${config.path}"
            val response = httpClient.request(versionedPath) {
                method = HttpMethod.Get
                config.parameters?.forEach { key, values ->
                    url.parameters.appendAll(key, values)
                }
            }.body<VisitsListResponse>()
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to fetch visits")
        }
    }

    override suspend fun getVisitById(id: String): ApiResult<VisitItem> {
        return try {
            val config = ApiEndpoints.Visits.getVisitById(id)
            val versionedPath = "v${config.version}/${config.path}"
            val response = httpClient.request(versionedPath) {
                method = HttpMethod.Get
                config.parameters?.forEach { key, values ->
                    url.parameters.appendAll(key, values)
                }
            }.body<VisitItem>()
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to fetch visit details")
        }
    }
}
