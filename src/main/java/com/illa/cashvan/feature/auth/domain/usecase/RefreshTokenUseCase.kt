package com.illa.cashvan.feature.auth.domain.usecase

import com.illa.cashvan.core.app_preferences.domain.use_case.token.SaveAccessTokenUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.token.SaveRefreshTokenUseCase
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.auth.domain.repository.AuthRepository

class RefreshTokenUseCase(
    private val authRepository: AuthRepository,
    private val saveTokenUseCase: SaveAccessTokenUseCase,
    private val saveRefreshTokenUseCase: SaveRefreshTokenUseCase
) {
    suspend operator fun invoke(): ApiResult<Unit> {
        return when (val result = authRepository.refreshToken()) {
            is ApiResult.Success -> {
                saveTokenUseCase(result.data.access_token)
                saveRefreshTokenUseCase(result.data.refresh_token)
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> result
            is ApiResult.Loading -> ApiResult.Loading
        }
    }
}
