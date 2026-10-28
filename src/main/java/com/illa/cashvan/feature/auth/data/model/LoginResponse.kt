package com.illa.cashvan.feature.auth.data.model

import kotlinx.serialization.Serializable

/**
 * `roles`, `agent_id` and `sales_agent_id` are new in the login contract and may be missing until the
 * backend ships them.
 */
@Serializable
data class LoginResponse(
    val access_token: String? = null,
    val refresh_token: String? = null,
    val user: User? = null,
    val message: String? = null,
    val roles: List<String>? = null,
    val agent_id: String? = null,
    val sales_agent_id: String? = null
)

@Serializable
data class User(
    val id: String,
    val phone_number: String,
    val name: String? = null,
    val email: String? = null,
    val agent_code: String? = null
)
