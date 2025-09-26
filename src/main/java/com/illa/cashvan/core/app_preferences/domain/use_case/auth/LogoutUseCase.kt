package com.illa.cashvan.core.app_preferences.domain.use_case.auth

import com.illa.cashvan.core.app_preferences.domain.use_case.app_cache.ClearAppDataUseCase
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.auth.domain.repository.AuthRepository

class LogoutUseCase(
    private val authRepository: AuthRepository,
    private val clearAppDataUseCase: ClearAppDataUseCase
) {
    suspend operator fun invoke(): ApiResult<Unit> {
        val apiResult = authRepository.logout()

        clearAppDataUseCase()

        return when (apiResult) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Error -> apiResult
            is ApiResult.Loading -> ApiResult.Loading
        }
    }
}