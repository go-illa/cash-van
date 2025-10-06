package com.illa.cashvan.core.app_preferences.domain.use_case.auth

import com.illa.cashvan.core.app_preferences.domain.use_case.app_cache.ClearAppDataUseCase
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.auth.domain.repository.AuthRepository

class LogoutUseCase(
    private val authRepository: AuthRepository,
    private val clearAppDataUseCase: ClearAppDataUseCase
) {
    suspend operator fun invoke(): ApiResult<Unit> {
        // Clear app data first to ensure all cached data is removed
        clearAppDataUseCase()

        // Then make the logout API call
        val apiResult = authRepository.logout()

        return when (apiResult) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Error -> {
                // Even if the API call fails, we've already cleared local data
                // So still return success to proceed with logout flow
                ApiResult.Success(Unit)
            }
            is ApiResult.Loading -> ApiResult.Loading
        }
    }
}