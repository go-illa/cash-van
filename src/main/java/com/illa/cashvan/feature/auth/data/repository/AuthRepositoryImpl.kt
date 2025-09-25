package com.illa.cashvan.feature.auth.data.repository

import com.illa.cashvan.core.network.endpoint.ApiEndpoints
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.auth.data.model.LoginRequest
import com.illa.cashvan.feature.auth.data.model.LoginResponse
import com.illa.cashvan.feature.auth.data.model.SalesAgent
import com.illa.cashvan.feature.auth.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
class AuthRepositoryImpl(
    private val httpClient: HttpClient
) : AuthRepository {

    override suspend fun login(phoneNumber: String, password: String): ApiResult<LoginResponse> {
        return try {
            val request = LoginRequest(
                sales_agent = SalesAgent(
                    phone_number = phoneNumber,
                    password = password
                )
            )

            val response = httpClient.post(ApiEndpoints.Auth.signIn().path) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<LoginResponse>()

            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e, e.message ?: "Login failed")
        }
    }
}