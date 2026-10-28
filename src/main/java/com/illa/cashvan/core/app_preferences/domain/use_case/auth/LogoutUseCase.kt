package com.illa.cashvan.core.app_preferences.domain.use_case.auth

import com.illa.cashvan.core.app_preferences.domain.use_case.app_cache.ClearAppDataUseCase
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.core.session.LogoutCleaner
import com.illa.cashvan.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.withTimeoutOrNull

class LogoutUseCase(
    private val authRepository: AuthRepository,
    private val clearAppDataUseCase: ClearAppDataUseCase,
    private val logoutCleaners: List<LogoutCleaner> = emptyList()
) {
    /** Clears every module's stored state. A slow network does not keep the agent signed in. */
    suspend operator fun invoke(): ApiResult<Unit> {
        withTimeoutOrNull(SERVER_LOGOUT_TIMEOUT_MS) { authRepository.logout() }
        clearAppDataUseCase()
        logoutCleaners.forEach { cleaner -> runCatching { cleaner.clear() } }
        return ApiResult.Success(Unit)
    }

    private companion object {
        const val SERVER_LOGOUT_TIMEOUT_MS = 3_000L
    }
}
