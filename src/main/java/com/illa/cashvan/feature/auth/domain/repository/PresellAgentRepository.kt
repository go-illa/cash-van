package com.illa.cashvan.feature.auth.domain.repository

import com.illa.cashvan.feature.auth.data.model.AgentLoginResponse
import com.illa.cashvan.feature.auth.data.model.PresellAgent

/**
 * Finds the Supabase `agents` row for a Rails sales agent. Only needed until the login response
 * carries `agent_id`.
 */
interface PresellAgentRepository {
    suspend fun findLinkedAgent(salesAgentId: String, phoneNumber: String?): PresellAgent?

    /** The sales PWA's sign-in: agent code and password checked by the Supabase `agent-login` function. */
    suspend fun agentLogin(agentCode: String, password: String): AgentLoginResponse
}
