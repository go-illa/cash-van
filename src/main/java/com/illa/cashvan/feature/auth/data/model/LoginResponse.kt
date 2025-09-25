package com.illa.cashvan.feature.auth.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val access_token: String? = null,
    val refresh_token: String? = null,
    val user: User? = null,
    val message: String? = null
)

@Serializable
data class User(
    val id: String,
    val phone_number: String,
    val name: String? = null,
    val email: String? = null
)