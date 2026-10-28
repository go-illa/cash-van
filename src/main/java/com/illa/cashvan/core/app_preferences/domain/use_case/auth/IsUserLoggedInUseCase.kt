package com.illa.cashvan.core.app_preferences.domain.use_case.auth

import com.illa.cashvan.core.app_preferences.domain.use_case.session.GetAgentSessionUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.token.GetTokenUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.user.GetUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class IsUserLoggedInUseCase(
    private val getTokenUseCase: GetTokenUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val getAgentSessionUseCase: GetAgentSessionUseCase
) {
    /** Signed in with Rails tokens, or as a presell-only agent whose session lives in Supabase alone. */
    suspend operator fun invoke(): Flow<Boolean> {
        return combine(
            getTokenUseCase(),
            getUserUseCase(),
            getAgentSessionUseCase()
        ) { token, user, session ->
            val presellOnly = session?.hasRailsAccess == false && session.agentId != null
            user != null && (!token.isNullOrBlank() || presellOnly)
        }
    }
}