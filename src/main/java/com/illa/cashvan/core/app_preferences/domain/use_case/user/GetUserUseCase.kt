package com.illa.cashvan.core.app_preferences.domain.use_case.user

import com.illa.cashvan.core.app_preferences.domain.repository.PreferencesRepo
import com.illa.cashvan.feature.auth.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.serializer

class GetUserUseCase(
    private val preferencesRepo: PreferencesRepo
) {
    suspend operator fun invoke(): Flow<User?> {
        return preferencesRepo.getValue(
            key = KEY_USER,
            defaultValue = null,
            serializer = serializer<User?>(),
            decrypt = true
        )
    }

    companion object {
        private const val KEY_USER = "user_data"
    }
}