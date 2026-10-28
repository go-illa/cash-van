package com.illa.cashvan.feature.auth.domain.usecase

import com.illa.cashvan.BuildConfig
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.core.session.AgentRole
import com.illa.cashvan.core.session.AgentSession
import com.illa.cashvan.feature.auth.data.model.LoginResponse
import com.illa.cashvan.feature.auth.data.model.PresellAgent
import com.illa.cashvan.feature.auth.data.model.User
import com.illa.cashvan.feature.auth.domain.model.LoginIdentifier
import com.illa.cashvan.feature.auth.domain.repository.AuthRepository
import com.illa.cashvan.feature.auth.domain.repository.PresellAgentRepository
import timber.log.Timber

sealed interface SalesLoginResult {
    data class Success(
        val session: AgentSession,
        val user: User,
        /** Null for a presell-only agent with no Rails (cash van) account. */
        val accessToken: String?,
        val refreshToken: String?
    ) : SalesLoginResult

    data class Failure(val message: String) : SalesLoginResult
}

/**
 * Signs a sales agent in the way the sales PWA does today, without the Rails login change: the agent code and
 * password go to Supabase `agent-login`, then the same password signs the agent's phone into Rails for cash van.
 * When that second step fails the agent is presell-only and cash van stays off, as in the PWA.
 */
class SalesAgentLoginUseCase(
    private val presellAgentRepository: PresellAgentRepository,
    private val authRepository: AuthRepository,
    private val mockRoles: String = BuildConfig.MOCK_ROLES
) {
    suspend operator fun invoke(agentCode: String, password: String): SalesLoginResult {
        val response = try {
            presellAgentRepository.agentLogin(agentCode, password)
        } catch (e: Exception) {
            Timber.e(e, "agent-login failed")
            return SalesLoginResult.Failure(LOGIN_FAILED)
        }
        val agent = response.agent
        if (!response.success || agent == null) return SalesLoginResult.Failure(agentLoginErrorMessage(response.errorKey))

        val railsLogin = railsPhone(agent.phone_number)?.let { phone ->
            (authRepository.login(LoginIdentifier.Phone(phone), password) as? ApiResult.Success)?.data
        }
        return buildSalesLogin(agent, railsLogin, mockRoles)
    }

    companion object {
        const val LOGIN_FAILED = "فشل تسجيل الدخول"
    }
}

/** The PWA's `agent-login` error keys in Arabic. */
internal fun agentLoginErrorMessage(errorKey: String?): String = when (errorKey) {
    "Invalid agent code or password" -> "رمز المندوب أو كلمة المرور غير صحيحة"
    "Your account is inactive. Please contact your supervisor." -> "حسابك غير مفعل. يرجى التواصل مع المشرف."
    else -> SalesAgentLoginUseCase.LOGIN_FAILED
}

/** `20` + the last 10 digits, the PWA's silent cash van login rule; null when the phone is too short. */
internal fun railsPhone(phone: String?): String? =
    phone?.filter(Char::isDigit)?.takeIf { it.length >= 10 }?.let { "20" + it.takeLast(10) }

/**
 * The session for an agent who signed in with their code. A Rails login with tokens and a user means cash van
 * works; otherwise the agent is presell-only. Fulfillment needs Rails, so it's dropped from mock roles without it.
 */
internal fun buildSalesLogin(agent: PresellAgent, railsLogin: LoginResponse?, mockRoles: String): SalesLoginResult.Success {
    val railsUser = railsLogin?.user?.takeIf { railsLogin.access_token != null }
    val hasRails = railsUser != null
    val mock = AgentRole.parse(mockRoles.split(',').map(String::trim).filter(String::isNotEmpty))
    val roles = buildSet {
        add(AgentRole.SALES)
        if (hasRails && mockRoles.isNotBlank() && AgentRole.FULFILLMENT in mock) add(AgentRole.FULFILLMENT)
    }
    return SalesLoginResult.Success(
        session = AgentSession(
            roles = roles.map(AgentRole::apiValue),
            agentId = agent.id,
            salesAgentId = railsUser?.id,
            agentCode = agent.agent_code,
            hasRailsAccess = hasRails
        ),
        user = railsUser ?: User(id = agent.id, phone_number = agent.phone_number.orEmpty(), name = agent.name, agent_code = agent.agent_code),
        accessToken = railsLogin?.access_token?.takeIf { hasRails },
        refreshToken = railsLogin?.refresh_token?.takeIf { hasRails }
    )
}
