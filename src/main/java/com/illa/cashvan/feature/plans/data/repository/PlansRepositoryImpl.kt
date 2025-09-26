package com.illa.cashvan.feature.plans.data.repository

import com.illa.cashvan.core.network.endpoint.ApiEndpoints
import com.illa.cashvan.core.network.endpoint.request
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.plans.data.model.PlansResponse
import com.illa.cashvan.feature.plans.domain.repository.PlansRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.request

class PlansRepositoryImpl(
    private val client: HttpClient
) : PlansRepository {

    override suspend fun getPlans(): ApiResult<PlansResponse> {
        return try {
            val response = client.request(ApiEndpoints.Plans.getPlans()).body<PlansResponse>()
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to fetch plans")
        }
    }
}