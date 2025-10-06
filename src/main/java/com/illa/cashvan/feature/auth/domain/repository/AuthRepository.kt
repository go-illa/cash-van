package com.illa.cashvan.feature.auth.domain.repository

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.auth.data.model.LoginResponse
import com.illa.cashvan.feature.auth.data.model.LogoutResponse
import com.illa.cashvan.feature.auth.data.model.RefreshTokenResponse

interface AuthRepository {
    suspend fun login(phoneNumber: String, password: String): ApiResult<LoginResponse>
    suspend fun logout(): ApiResult<LogoutResponse>
    suspend fun refreshToken(): ApiResult<RefreshTokenResponse>
}