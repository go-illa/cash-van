package com.illa.cashvan.feature.auth.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AgentLoginRequest(
    val agent_code: String,
    val password: String
)

/** The sales PWA's `agent-login` edge function: `success` with the agent, or an `errorKey`. */
@Serializable
data class AgentLoginResponse(
    val success: Boolean = false,
    val errorKey: String? = null,
    val agent: PresellAgent? = null
)
