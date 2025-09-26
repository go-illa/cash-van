package com.illa.cashvan.feature.auth.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LogoutResponse(
    val message: String? = null,
    val success: Boolean = true
)