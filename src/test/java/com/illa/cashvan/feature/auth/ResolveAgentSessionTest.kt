package com.illa.cashvan.feature.auth

import com.illa.cashvan.core.session.AgentRole
import com.illa.cashvan.core.session.AgentSession
import com.illa.cashvan.feature.auth.data.model.LoginResponse
import com.illa.cashvan.feature.auth.data.model.PresellAgent
import com.illa.cashvan.feature.auth.data.model.User
import com.illa.cashvan.feature.auth.domain.usecase.AgentSessionResult
import com.illa.cashvan.feature.auth.domain.usecase.buildAgentSession
import com.illa.cashvan.feature.auth.domain.usecase.needsPresellLookup
import com.illa.cashvan.feature.auth.domain.usecase.rolesFrom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveAgentSessionTest {

    private val user = User(id = "sa-1", phone_number = "201012345678", agent_code = null)
    private val both = setOf(AgentRole.SALES, AgentRole.FULFILLMENT)

    private fun response(roles: List<String>? = null, agentId: String? = null) =
        LoginResponse(access_token = "t", refresh_token = "r", user = user, roles = roles, agent_id = agentId)

    @Test
    fun `backend roles win over mock roles`() {
        assertEquals(setOf(AgentRole.FULFILLMENT), rolesFrom(response(roles = listOf("fulfillment")), "sales"))
    }

    @Test
    fun `mock roles apply when the backend sends none`() {
        assertEquals(both, rolesFrom(response(), "sales, fulfillment"))
        assertEquals(setOf(AgentRole.FULFILLMENT), rolesFrom(response(), ""))
    }

    @Test
    fun `lookup is only needed for sales without an agent id`() {
        assertTrue(needsPresellLookup(both, response()))
        assertFalse(needsPresellLookup(both, response(agentId = "a-1")))
        assertFalse(needsPresellLookup(setOf(AgentRole.FULFILLMENT), response()))
    }

    @Test
    fun `fulfillment agents need no presell agent`() {
        val result = buildAgentSession(response(), setOf(AgentRole.FULFILLMENT), presellAgent = null)
        assertEquals(
            AgentSessionResult.Success(AgentSession(roles = listOf("fulfillment"), salesAgentId = "sa-1")),
            result
        )
    }

    @Test
    fun `agent id from the backend is used as is`() {
        val result = buildAgentSession(response(agentId = "a-1"), both, presellAgent = null)
        assertEquals("a-1", (result as AgentSessionResult.Success).session.agentId)
    }

    @Test
    fun `active presell agent fills agent id and code`() {
        val agent = PresellAgent(id = "a-1", agent_code = "ILLA_001", status = 1)
        val session = (buildAgentSession(response(), both, agent) as AgentSessionResult.Success).session
        assertEquals("a-1", session.agentId)
        assertEquals("ILLA_001", session.agentCode)
        assertEquals(both, session.agentRoles)
    }

    @Test
    fun `missing presell agent drops the sales role`() {
        val session = (buildAgentSession(response(), both, presellAgent = null) as AgentSessionResult.Success).session
        assertEquals(listOf("fulfillment"), session.roles)
        assertEquals(null, session.agentId)
    }

    @Test
    fun `sales only agent without a presell agent cannot sign in`() {
        assertEquals(
            AgentSessionResult.NoPresellAccount,
            buildAgentSession(response(), setOf(AgentRole.SALES), presellAgent = null)
        )
    }

    @Test
    fun `inactive sales only agent cannot sign in`() {
        val inactive = PresellAgent(id = "a-1", status = 0)
        assertEquals(
            AgentSessionResult.InactiveAgent,
            buildAgentSession(response(), setOf(AgentRole.SALES), inactive)
        )
    }
}
