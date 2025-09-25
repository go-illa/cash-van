package com.illa.cashvan.core.app_preferences.domain.use_case.app_cache

import com.illa.cashvan.core.app_preferences.domain.repository.PreferencesRepo

class ClearAppDataUseCase(
    private val preferencesRepo: PreferencesRepo
) {
    suspend operator fun invoke() {
        preferencesRepo.clearAppData()
    }
}