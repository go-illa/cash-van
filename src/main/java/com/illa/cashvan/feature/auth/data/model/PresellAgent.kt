package com.illa.cashvan.feature.auth.data.model

import kotlinx.serialization.Serializable

/** A row of the Supabase `agents` table, which presell calls are keyed on. */
@Serializable
data class PresellAgent(
    val id: String,
    val agent_code: String? = null,
    val name: String? = null,
    val status: Int? = null,
    val phone_number: String? = null
) {
    val isActive: Boolean get() = status == ACTIVE_STATUS

    companion object {
        const val ACTIVE_STATUS = 1
    }
}

@Serializable
data class SalesAgentLink(
    val agent_id: String? = null
)
