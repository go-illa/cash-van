package com.illa.cashvan.core.app_preferences.domain.use_case.user

import com.illa.cashvan.core.app_preferences.domain.repository.PreferencesRepo
import com.illa.cashvan.feature.auth.data.model.User
import kotlinx.serialization.serializer

class SaveUserUseCase(
    private val preferencesRepo: PreferencesRepo
) {
    suspend operator fun invoke(user: User) {
        preferencesRepo.setValue(
            key = KEY_USER,
            value = user,
            serializer = serializer(),
            encrypt = true
        )
    }

    companion object {
        private const val KEY_USER = "user_data"
    }
}