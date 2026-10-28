package com.illa.cashvan.feature.auth.data.repository

import com.illa.cashvan.core.supabase.SupabaseClient
import com.illa.cashvan.feature.auth.data.model.AgentLoginRequest
import com.illa.cashvan.feature.auth.data.model.AgentLoginResponse
import com.illa.cashvan.feature.auth.data.model.PresellAgent
import com.illa.cashvan.feature.auth.data.model.SalesAgentLink
import com.illa.cashvan.feature.auth.domain.repository.PresellAgentRepository

class PresellAgentRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : PresellAgentRepository {

    /** Tries `sales_agents.agent_id` first, then matches `agents.phone_number` on its last 10 digits like the PWA. */
    override suspend fun findLinkedAgent(salesAgentId: String, phoneNumber: String?): PresellAgent? {
        val linkedId = supabaseClient.selectFirstOrNull<SalesAgentLink>("sales_agents") {
            select("agent_id")
            eq("id", salesAgentId)
        }?.agent_id

        if (linkedId != null) {
            supabaseClient.selectFirstOrNull<PresellAgent>("agents") {
                select(AGENT_COLUMNS)
                eq("id", linkedId)
            }?.let { return it }
        }

        val last10 = phoneNumber?.lastTenDigits() ?: return null
        return supabaseClient.select<PresellAgent>("agents") {
            select(AGENT_COLUMNS)
            ilike("phone_number", "*$last10*")
            limit(PHONE_MATCH_LIMIT)
        }.firstOrNull { it.phone_number?.lastTenDigits() == last10 }
    }

    override suspend fun agentLogin(agentCode: String, password: String): AgentLoginResponse =
        supabaseClient.invokeFunction<AgentLoginRequest, AgentLoginResponse>("agent-login", AgentLoginRequest(agentCode, password))

    private fun String.lastTenDigits(): String? =
        filter(Char::isDigit).takeIf { it.length >= 10 }?.takeLast(10)

    private companion object {
        const val AGENT_COLUMNS = "id,agent_code,name,status,phone_number"
        const val PHONE_MATCH_LIMIT = 50
    }
}
