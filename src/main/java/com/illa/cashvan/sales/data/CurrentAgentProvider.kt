package com.illa.cashvan.sales.data

import com.illa.cashvan.core.app_preferences.domain.use_case.session.GetAgentSessionUseCase
import kotlinx.coroutines.flow.firstOrNull

/**
 * @property agentId Supabase `agents.id`, the key for presell data.
 * @property salesAgentId Rails `sales_agents.id`; cash van visits are recorded against it.
 */
data class SalesAgentIdentity(
    val agentId: String,
    val salesAgentId: String?,
    val agentCode: String?,
    /** False for presell-only agents: cash van and every other Rails call is off. */
    val hasRailsAccess: Boolean = true
)

class CurrentAgentProvider(
    private val getAgentSessionUseCase: GetAgentSessionUseCase
) {
    /** Sign-in only opens the Sales module with an agent id, so a missing one means a broken session. */
    suspend fun require(): SalesAgentIdentity {
        val session = getAgentSessionUseCase().firstOrNull()
        val agentId = session?.agentId ?: throw IllegalStateException("Sales session has no agent id")
        return SalesAgentIdentity(agentId, session.salesAgentId, session.agentCode, session.hasRailsAccess)
    }
}
