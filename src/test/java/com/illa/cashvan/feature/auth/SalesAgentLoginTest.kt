package com.illa.cashvan.feature.auth

import com.illa.cashvan.core.session.AgentRole
import com.illa.cashvan.feature.auth.data.model.LoginResponse
import com.illa.cashvan.feature.auth.data.model.PresellAgent
import com.illa.cashvan.feature.auth.data.model.User
import com.illa.cashvan.feature.auth.domain.usecase.agentLoginErrorMessage
import com.illa.cashvan.feature.auth.domain.usecase.buildSalesLogin
import com.illa.cashvan.feature.auth.domain.usecase.railsPhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SalesAgentLoginTest {

    private val agent = PresellAgent(id = "a-1", agent_code = "ILLA_001", name = "Ahmed", status = 1, phone_number = "01012345678")
    private val railsLogin = LoginResponse(
        access_token = "access",
        refresh_token = "refresh",
        user = User(id = "sa-1", phone_number = "201012345678", name = "Ahmed")
    )

    @Test
    fun `the agent's phone signs into Rails as 20 plus the last 10 digits`() {
        assertEquals("201012345678", railsPhone("01012345678"))
        assertEquals("201012345678", railsPhone("+20 101 234 5678"))
        assertNull(railsPhone("12345"))
        assertNull(railsPhone(null))
    }

    @Test
    fun `agent-login error keys show the PWA's Arabic messages`() {
        assertEquals("رمز المندوب أو كلمة المرور غير صحيحة", agentLoginErrorMessage("Invalid agent code or password"))
        assertEquals("حسابك غير مفعل. يرجى التواصل مع المشرف.", agentLoginErrorMessage("Your account is inactive. Please contact your supervisor."))
        assertEquals("فشل تسجيل الدخول", agentLoginErrorMessage(null))
    }

    @Test
    fun `with a Rails account the agent gets cash van and Rails tokens`() {
        val result = buildSalesLogin(agent, railsLogin, mockRoles = "")
        assertTrue(result.session.hasRailsAccess)
        assertEquals("a-1", result.session.agentId)
        assertEquals("sa-1", result.session.salesAgentId)
        assertEquals("ILLA_001", result.session.agentCode)
        assertEquals(setOf(AgentRole.SALES), result.session.agentRoles)
        assertEquals("access", result.accessToken)
        assertEquals("sa-1", result.user.id)
    }

    @Test
    fun `without a Rails account the agent is presell-only`() {
        val result = buildSalesLogin(agent, railsLogin = null, mockRoles = "sales,fulfillment")
        assertFalse(result.session.hasRailsAccess)
        assertEquals(setOf(AgentRole.SALES), result.session.agentRoles)
        assertNull(result.accessToken)
        assertNull(result.session.salesAgentId)
        assertEquals("a-1", result.user.id)
        assertEquals("01012345678", result.user.phone_number)
    }

    @Test
    fun `mock roles add fulfillment only when Rails works`() {
        val result = buildSalesLogin(agent, railsLogin, mockRoles = "sales,fulfillment")
        assertEquals(setOf(AgentRole.SALES, AgentRole.FULFILLMENT), result.session.agentRoles)
    }
}
