package com.illa.cashvan.core.app_preferences.domain.use_case.token

import com.illa.cashvan.core.app_preferences.domain.repository.PreferencesRepo

class ClearRefreshTokenUseCase(
    private val preferencesRepo: PreferencesRepo
) {
    companion object {
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }

    suspend operator fun invoke() {
        preferencesRepo.clearPreference(REFRESH_TOKEN_KEY)
    }
}