package com.illa.cashvan.feature.auth.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LogoutRequest(
    val tokens: TokenData
)

@Serializable
data class TokenData(
    val access_token: String,
    val refresh_token: String
)