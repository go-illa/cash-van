package com.illa.cashvan.feature.auth.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val sales_agent: SalesAgent
)

@Serializable
data class SalesAgent(
    val phone_number: String,
    val password: String
)