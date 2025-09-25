package com.illa.cashvan.feature.auth.domain.usecase

import com.illa.cashvan.core.app_preferences.domain.use_case.app_cache.ClearAppDataUseCase
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.auth.domain.repository.AuthRepository

class LogoutUseCase(
    private val authRepository: AuthRepository,
    private val clearAppDataUseCase: ClearAppDataUseCase
) {
    suspend operator fun invoke(): ApiResult<Unit> {
        // Call the logout API first
        val apiResult = authRepository.logout()

        // Regardless of API result, clear local data
        clearAppDataUseCase()

        return apiResult
    }
}