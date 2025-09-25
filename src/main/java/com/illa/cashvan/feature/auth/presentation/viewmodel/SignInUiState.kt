package com.illa.cashvan.feature.auth.presentation.viewmodel

import com.illa.cashvan.feature.auth.data.model.User

data class SignInUiState(
    val isLoading: Boolean = false,
    val isLoginSuccessful: Boolean = false,
    val error: String? = null,
    val user: User? = null
)