package com.illa.cashvan.core.session

import kotlinx.serialization.Serializable

/**
 * Identity the app keeps beside the Rails tokens.
 *
 * @property agentId Supabase `agents.id`, needed for every presell call. Null for fulfillment-only agents.
 * @property salesAgentId Rails `sales_agents.id`.
 * @property hasRailsAccess false for a presell-only agent signed in through Supabase `agent-login` with no Rails
 * (cash van) account: there are no Rails tokens, so nothing may call Rails for them.
 */
@Serializable
data class AgentSession(
    val roles: List<String> = emptyList(),
    val agentId: String? = null,
    val salesAgentId: String? = null,
    val agentCode: String? = null,
    val hasRailsAccess: Boolean = true
) {
    val agentRoles: Set<AgentRole> get() = AgentRole.parse(roles)
}
