package com.illa.cashvan.feature.auth.domain.usecase

import com.illa.cashvan.BuildConfig
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.core.session.AgentRole
import com.illa.cashvan.core.session.AgentSession
import com.illa.cashvan.feature.auth.data.model.LoginResponse
import com.illa.cashvan.feature.auth.data.model.PresellAgent
import com.illa.cashvan.feature.auth.domain.repository.PresellAgentRepository
import timber.log.Timber

/**
 * Turns a login response into the [AgentSession] the app stores: which modules the agent can open and the
 * Supabase agent id the Sales module needs.
 */
class ResolveAgentSessionUseCase(
    private val presellAgentRepository: PresellAgentRepository,
    private val mockRoles: String = BuildConfig.MOCK_ROLES
) {
    suspend operator fun invoke(response: LoginResponse): ApiResult<AgentSession> {
        val user = response.user ?: return ApiResult.Error(IllegalStateException("No user"), LOGIN_FAILED)
        val roles = rolesFrom(response, mockRoles)

        val presellAgent = if (needsPresellLookup(roles, response)) {
            try {
                presellAgentRepository.findLinkedAgent(response.sales_agent_id ?: user.id, user.phone_number)
            } catch (e: Exception) {
                Timber.e(e, "Presell agent lookup failed")
                return ApiResult.Error(e, PRESELL_LOOKUP_FAILED)
            }
        } else {
            null
        }

        return when (val result = buildAgentSession(response, roles, presellAgent)) {
            is AgentSessionResult.Success -> ApiResult.Success(result.session)
            AgentSessionResult.NoPresellAccount -> ApiResult.Error(IllegalStateException("No presell agent"), NO_PRESELL_ACCOUNT)
            AgentSessionResult.InactiveAgent -> ApiResult.Error(IllegalStateException("Inactive agent"), INACTIVE_AGENT)
        }
    }

    companion object {
        const val LOGIN_FAILED = "فشل تسجيل الدخول"
        const val PRESELL_LOOKUP_FAILED = "تعذر التحقق من حساب البري سيل. حاول مرة أخرى"
        const val NO_PRESELL_ACCOUNT = "انت حسابك مش مربوط بحساب بري-سيل كلم المشرف بتاعك"
        const val INACTIVE_AGENT = "حسابك غير مفعل. يرجى التواصل مع المشرف."
    }
}

sealed interface AgentSessionResult {
    data class Success(val session: AgentSession) : AgentSessionResult
    data object NoPresellAccount : AgentSessionResult
    data object InactiveAgent : AgentSessionResult
}

/** Backend roles win; [mockRoles] (comma separated) stands in until the backend sends them. */
internal fun rolesFrom(response: LoginResponse, mockRoles: String): Set<AgentRole> {
    val mock = mockRoles.split(',').map(String::trim).filter(String::isNotEmpty)
    return AgentRole.parse(response.roles ?: mock)
}

internal fun needsPresellLookup(roles: Set<AgentRole>, response: LoginResponse): Boolean =
    AgentRole.SALES in roles && response.agent_id.isNullOrBlank()

/**
 * A sales role is only usable with an active Supabase agent. Without one, the agent keeps their other
 * roles; an agent left with no role cannot sign in.
 */
internal fun buildAgentSession(
    response: LoginResponse,
    roles: Set<AgentRole>,
    presellAgent: PresellAgent?
): AgentSessionResult {
    val user = response.user
    val session = AgentSession(
        salesAgentId = response.sales_agent_id ?: user?.id,
        agentCode = user?.agent_code
    )

    if (!needsPresellLookup(roles, response)) {
        return AgentSessionResult.Success(
            session.copy(roles = roles.map(AgentRole::apiValue), agentId = response.agent_id)
        )
    }

    if (presellAgent != null && presellAgent.isActive) {
        return AgentSessionResult.Success(
            session.copy(
                roles = roles.map(AgentRole::apiValue),
                agentId = presellAgent.id,
                agentCode = session.agentCode ?: presellAgent.agent_code
            )
        )
    }

    val remaining = roles - AgentRole.SALES
    return when {
        remaining.isNotEmpty() -> AgentSessionResult.Success(session.copy(roles = remaining.map(AgentRole::apiValue)))
        presellAgent == null -> AgentSessionResult.NoPresellAccount
        else -> AgentSessionResult.InactiveAgent
    }
}
