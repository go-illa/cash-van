package com.illa.cashvan.feature.auth.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenRequest(
    val tokens: RefreshTokenData
)

@Serializable
data class RefreshTokenData(
    val refresh_token: String
)
