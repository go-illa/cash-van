package com.illa.cashvan.core.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.app_preferences.domain.use_case.auth.IsUserLoggedInUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.auth.LogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthenticationState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = true
)

class AuthenticationViewModel(
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthenticationState())
    val authState: StateFlow<AuthenticationState> = _authState.asStateFlow()

    init {
        checkAuthenticationStatus()
    }

    private fun checkAuthenticationStatus() {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true)
            isUserLoggedInUseCase().collect { isLoggedIn ->
                _authState.value = AuthenticationState(
                    isLoggedIn = isLoggedIn,
                    isLoading = false
                )
            }
        }
    }

    fun refreshAuthState() {
        checkAuthenticationStatus()
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            checkAuthenticationStatus()
        }
    }
}