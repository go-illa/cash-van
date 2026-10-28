package com.illa.cashvan.feature.auth.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.auth.data.model.LoginResponse
import com.illa.cashvan.feature.auth.domain.model.LoginIdentifier
import com.illa.cashvan.feature.auth.domain.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(identifier: LoginIdentifier, password: String): ApiResult<LoginResponse> {
        return authRepository.login(identifier, password)
    }
}
