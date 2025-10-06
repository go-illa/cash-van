package com.illa.cashvan.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.analytics.CashVanAnalyticsHelper
import com.illa.cashvan.core.app_preferences.domain.use_case.token.SaveAccessTokenUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.token.SaveRefreshTokenUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.user.SaveUserUseCase
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.auth.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignInViewModel(
    private val loginUseCase: LoginUseCase,
    private val saveAccessTokenUseCase: SaveAccessTokenUseCase,
    private val saveRefreshTokenUseCase: SaveRefreshTokenUseCase,
    private val saveUserUseCase: SaveUserUseCase,
    private val analyticsHelper: CashVanAnalyticsHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun login(phoneNumber: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Add "2" prefix to phone number
            val formattedPhoneNumber = "2$phoneNumber"

            when (val result = loginUseCase(formattedPhoneNumber, password)) {
                is ApiResult.Success -> {
                    result.data.access_token?.let {
                        saveAccessTokenUseCase(it)
                    }
                    result.data.refresh_token?.let {
                        saveRefreshTokenUseCase(it)
                    }
                    result.data.user?.let {
                        saveUserUseCase(it)
                    }

                    // Identify user in analytics
                    result.data.user?.phone_number?.let { phone ->
                        analyticsHelper.identify(phone)
                        analyticsHelper.logEvent(
                            "sign_in",
                            mapOf(
                                "phone" to phone,
                                "user_id" to (result.data.user.id)
                            )
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoginSuccessful = true,
                        user = result.data.user
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetLoginState() {
        _uiState.value = _uiState.value.copy(isLoginSuccessful = false)
    }
}