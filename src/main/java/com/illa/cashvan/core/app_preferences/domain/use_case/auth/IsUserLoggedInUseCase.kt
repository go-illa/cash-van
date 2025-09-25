package com.illa.cashvan.core.app_preferences.domain.use_case.auth

import com.illa.cashvan.core.app_preferences.domain.use_case.token.GetTokenUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.user.GetUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class IsUserLoggedInUseCase(
    private val getTokenUseCase: GetTokenUseCase,
    private val getUserUseCase: GetUserUseCase
) {
    suspend operator fun invoke(): Flow<Boolean> {
        return combine(
            getTokenUseCase(),
            getUserUseCase()
        ) { token, user ->
            !token.isNullOrBlank() && user != null
        }
    }
}