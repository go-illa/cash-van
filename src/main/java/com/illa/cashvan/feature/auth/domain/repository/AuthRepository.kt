package com.illa.cashvan.feature.auth.domain.repository

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.auth.data.model.LoginResponse

interface AuthRepository {
    suspend fun login(phoneNumber: String, password: String): ApiResult<LoginResponse>
}