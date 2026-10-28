package com.illa.cashvan.core.session

/**
 * State a module keeps outside preferences (e.g. the Sales cart database) that logout must wipe.
 * Register implementations in Koin; [com.illa.cashvan.core.app_preferences.domain.use_case.auth.LogoutUseCase]
 * runs them all.
 */
fun interface LogoutCleaner {
    suspend fun clear()
}
