package com.illa.cashvan.core.app_preferences.domain.use_case.token

import com.illa.cashvan.core.app_preferences.domain.repository.PreferencesRepo
import kotlinx.coroutines.flow.Flow

class GetTokenUseCase(
    private val preferencesRepo: PreferencesRepo
) {
    companion object {
        private const val ACCESS_TOKEN_KEY = "access_token"
    }

    suspend operator fun invoke(): Flow<String> {
        return preferencesRepo.getValue(ACCESS_TOKEN_KEY, "")
    }
}