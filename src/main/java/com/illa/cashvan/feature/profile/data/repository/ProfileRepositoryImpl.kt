package com.illa.cashvan.feature.profile.data.repository

import com.illa.cashvan.core.network.endpoint.ApiEndpoints
import com.illa.cashvan.core.network.endpoint.request
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.profile.data.model.ProfileResponse
import com.illa.cashvan.feature.profile.domain.repository.ProfileRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body

class ProfileRepositoryImpl(
    private val httpClient: HttpClient
) : ProfileRepository {

    override suspend fun getProfile(salesAgentId: String): ApiResult<ProfileResponse> {
        return try {
            val response = httpClient.request(ApiEndpoints.Profile.getProfile(salesAgentId))
                .body<ProfileResponse>()

            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Failed to get profile")
        }
    }
}