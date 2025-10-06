package com.illa.cashvan.feature.auth.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenResponse(
    val access_token: String,
    val refresh_token: String
)
