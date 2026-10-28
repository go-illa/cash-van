package com.illa.cashvan.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.analytics.CashVanAnalyticsHelper
import com.illa.cashvan.core.analytics.SalesAnalytics
import com.illa.cashvan.core.app_preferences.domain.use_case.session.SaveAgentSessionUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.token.SaveAccessTokenUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.token.SaveRefreshTokenUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.user.SaveUserUseCase
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.core.session.AgentRole
import com.illa.cashvan.core.session.AgentSession
import com.illa.cashvan.feature.auth.data.model.User
import com.illa.cashvan.feature.auth.domain.model.LoginIdentifier
import com.illa.cashvan.feature.auth.domain.usecase.LoginUseCase
import com.illa.cashvan.feature.auth.domain.usecase.ResolveAgentSessionUseCase
import com.illa.cashvan.feature.auth.domain.usecase.SalesAgentLoginUseCase
import com.illa.cashvan.feature.auth.domain.usecase.SalesLoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignInViewModel(
    private val loginUseCase: LoginUseCase,
    private val resolveAgentSessionUseCase: ResolveAgentSessionUseCase,
    private val salesAgentLoginUseCase: SalesAgentLoginUseCase,
    private val saveAccessTokenUseCase: SaveAccessTokenUseCase,
    private val saveRefreshTokenUseCase: SaveRefreshTokenUseCase,
    private val saveUserUseCase: SaveUserUseCase,
    private val saveAgentSessionUseCase: SaveAgentSessionUseCase,
    private val analyticsHelper: CashVanAnalyticsHelper,
    private val salesAnalytics: SalesAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun login(identifierInput: String, password: String) {
        val identifier = LoginIdentifier.parse(identifierInput)
        if (identifier == null) {
            _uiState.value = _uiState.value.copy(error = EMPTY_IDENTIFIER)
            return
        }
        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = EMPTY_PASSWORD)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (identifier) {
                is LoginIdentifier.AgentCode -> loginWithAgentCode(identifier.value, password)
                is LoginIdentifier.Phone -> loginWithPhone(identifier, password)
            }
        }
    }

    /** Sales agents sign in with their code, as in the sales PWA. */
    private suspend fun loginWithAgentCode(agentCode: String, password: String) {
        when (val result = salesAgentLoginUseCase(agentCode, password)) {
            is SalesLoginResult.Success -> completeSignIn(result.accessToken, result.refreshToken, result.user, result.session)
            is SalesLoginResult.Failure -> {
                salesAnalytics.track("agent_login_failed", "agent_code" to agentCode, "error_key" to result.message)
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    /** Phone sign-in against Rails: today's fulfillment login. */
    private suspend fun loginWithPhone(identifier: LoginIdentifier, password: String) {
        when (val result = loginUseCase(identifier, password)) {
            is ApiResult.Success -> {
                // Resolve roles before saving anything, so a rejected agent is not left half signed in.
                val session = when (val resolved = resolveAgentSessionUseCase(result.data)) {
                    is ApiResult.Success -> resolved.data
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = resolved.message)
                        return
                    }
                    is ApiResult.Loading -> return
                }
                val user = result.data.user
                if (user == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = ResolveAgentSessionUseCase.LOGIN_FAILED)
                    return
                }
                completeSignIn(result.data.access_token, result.data.refresh_token, user, session)
            }
            is ApiResult.Error -> {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
            is ApiResult.Loading -> {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
        }
    }

    private suspend fun completeSignIn(accessToken: String?, refreshToken: String?, user: User, session: AgentSession) {
        accessToken?.let { saveAccessTokenUseCase(it) }
        refreshToken?.let { saveRefreshTokenUseCase(it) }
        saveUserUseCase(user)
        saveAgentSessionUseCase(session)

        if (user.phone_number.isNotBlank()) {
            analyticsHelper.identify(user.phone_number)
            analyticsHelper.logEvent(
                "sign_in",
                mapOf(
                    "phone" to user.phone_number,
                    "user_id" to user.id,
                    "roles" to session.roles.joinToString(",")
                )
            )
        }
        val agentId = session.agentId
        if (AgentRole.SALES in session.agentRoles && agentId != null) {
            val agentCode = session.agentCode ?: user.phone_number
            salesAnalytics.identify(agentCode, agentId, user.name)
            salesAnalytics.track("agent_sign_in", "agent_id" to agentId, "agent_name" to user.name, "agent_code" to agentCode)
        }

        _uiState.value = _uiState.value.copy(isLoading = false, isLoginSuccessful = true, user = user)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetLoginState() {
        _uiState.value = _uiState.value.copy(isLoginSuccessful = false)
    }

    private companion object {
        const val EMPTY_IDENTIFIER = "من فضلك ادخل رقم الهاتف أو كود المندوب"
        const val EMPTY_PASSWORD = "من فضلك ادخل كلمة السر"
    }
}
