package com.illa.cashvan.core.app_preferences.domain.use_case.app_cache

import com.illa.cashvan.core.app_preferences.domain.repository.PreferencesRepo
import android.util.Log

class ClearAppDataUseCase(
    private val preferencesRepo: PreferencesRepo
) {
    suspend operator fun invoke() {
        try {
            Log.d("ClearAppDataUseCase", "Clearing all app data due to authentication failure")
            preferencesRepo.clearAppData()
            Log.d("ClearAppDataUseCase", "App data cleared successfully")
        } catch (e: Exception) {
            Log.e("ClearAppDataUseCase", "Error clearing app data", e)
        }
    }
}