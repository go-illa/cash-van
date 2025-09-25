package com.illa.cashvan.core.app_preferences.domain.use_case.user

import com.illa.cashvan.core.app_preferences.domain.repository.PreferencesRepo

class ClearUserUseCase(
    private val preferencesRepo: PreferencesRepo
) {
    suspend operator fun invoke() {
        preferencesRepo.clearPreference(KEY_USER)
    }

    companion object {
        private const val KEY_USER = "user_data"
    }
}