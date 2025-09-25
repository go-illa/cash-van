package com.illa.cashvan.core.app_preferences.domain.use_case.token

import com.illa.cashvan.core.app_preferences.domain.repository.PreferencesRepo

class SaveRefreshTokenUseCase(
    private val preferencesRepo: PreferencesRepo
) {
    companion object {
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }

    suspend operator fun invoke(token: String) {
        preferencesRepo.setValue(REFRESH_TOKEN_KEY, token)
    }
}