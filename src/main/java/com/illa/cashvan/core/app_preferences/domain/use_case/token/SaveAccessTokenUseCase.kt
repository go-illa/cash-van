package com.illa.cashvan.core.app_preferences.domain.use_case.token

import com.illa.cashvan.core.app_preferences.domain.repository.PreferencesRepo

class SaveAccessTokenUseCase(
    private val preferencesRepo: PreferencesRepo
) {
    companion object {
        private const val ACCESS_TOKEN_KEY = "access_token"
    }

    suspend operator fun invoke(token: String) {
        preferencesRepo.setValue(ACCESS_TOKEN_KEY, token)
    }
}