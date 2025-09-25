package com.illa.cashvan.core.app_preferences.domain.use_case.token

import com.illa.cashvan.core.app_preferences.domain.repository.PreferencesRepo
import kotlinx.coroutines.flow.Flow

class GetRefreshTokenUseCase(
    private val preferencesRepo: PreferencesRepo
) {
    companion object {
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }

    suspend operator fun invoke(): Flow<String> {
        return preferencesRepo.getValue(REFRESH_TOKEN_KEY, "")
    }
}