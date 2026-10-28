package com.illa.cashvan.feature.auth.data.model

import com.illa.cashvan.feature.auth.domain.model.LoginIdentifier
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val sales_agent: SalesAgent
)

/**
 * Phones go in `phone_number`, which every backend version accepts. Agent codes go in `login`.
 */
@Serializable
data class SalesAgent(
    val password: String,
    val phone_number: String? = null,
    val login: String? = null
) {
    companion object {
        fun from(identifier: LoginIdentifier, password: String) = when (identifier) {
            is LoginIdentifier.Phone -> SalesAgent(password = password, phone_number = identifier.value)
            is LoginIdentifier.AgentCode -> SalesAgent(password = password, login = identifier.value)
        }
    }
}
