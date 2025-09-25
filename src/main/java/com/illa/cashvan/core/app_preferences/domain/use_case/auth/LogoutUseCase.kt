package com.illa.cashvan.core.app_preferences.domain.use_case.auth

import com.illa.cashvan.core.app_preferences.domain.use_case.app_cache.ClearAppDataUseCase

class LogoutUseCase(
    private val clearAppDataUseCase: ClearAppDataUseCase
) {
    suspend operator fun invoke() {
        clearAppDataUseCase()
    }
}