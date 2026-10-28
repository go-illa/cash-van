package com.illa.cashvan.core.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.app_preferences.domain.use_case.auth.IsUserLoggedInUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.auth.LogoutUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.session.GetActiveModuleUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.session.GetAgentSessionUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.session.SaveActiveModuleUseCase
import com.illa.cashvan.core.session.AgentRole
import com.illa.cashvan.core.session.SessionManager
import com.illa.cashvan.core.session.resolveStartModule
import com.illa.cashvan.feature.auth.domain.usecase.RefreshAgentRolesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class AuthenticationState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = true,
    val roles: Set<AgentRole> = emptySet(),
    /** The module to show. Null while signed out, or while an agent with both roles picks one. */
    val activeModule: AgentRole? = null
) {
    val canSwitchModule: Boolean get() = roles.size > 1
}

class AuthenticationViewModel(
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val sessionManager: SessionManager,
    private val getAgentSessionUseCase: GetAgentSessionUseCase,
    private val getActiveModuleUseCase: GetActiveModuleUseCase,
    private val saveActiveModuleUseCase: SaveActiveModuleUseCase,
    private val refreshAgentRolesUseCase: RefreshAgentRolesUseCase
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthenticationState())
    val authState: StateFlow<AuthenticationState> = _authState.asStateFlow()

    private var authJob: Job? = null

    init {
        checkAuthenticationStatus(refreshRoles = true)
        observeForceLogout()
    }

    private fun observeForceLogout() {
        viewModelScope.launch {
            sessionManager.forceLogoutEvents.collect {
                checkAuthenticationStatus()
            }
        }
    }

    private fun checkAuthenticationStatus(refreshRoles: Boolean = false) {
        authJob?.cancel()
        authJob = viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true)
            if (refreshRoles && isUserLoggedInUseCase().first()) {
                // Runs during the splash delay; a slow network keeps the stored roles.
                withTimeoutOrNull(ROLE_REFRESH_TIMEOUT_MS) { refreshAgentRolesUseCase() }
            }
            isUserLoggedInUseCase().distinctUntilChanged().collect { isLoggedIn ->
                _authState.value = if (isLoggedIn) loggedInState() else AuthenticationState(isLoading = false)
            }
        }
    }

    private suspend fun loggedInState(): AuthenticationState {
        val roles = getAgentSessionUseCase().firstOrNull()?.agentRoles ?: AgentRole.parse(null)
        val lastActive = getActiveModuleUseCase().firstOrNull()
        return AuthenticationState(
            isLoggedIn = true,
            isLoading = false,
            roles = roles,
            activeModule = resolveStartModule(roles, lastActive)
        )
    }

    fun refreshAuthState() {
        checkAuthenticationStatus()
    }

    fun selectModule(module: AgentRole) {
        viewModelScope.launch {
            saveActiveModuleUseCase(module)
            _authState.update { it.copy(activeModule = module) }
        }
    }

    /** Back to the module picker. The stored choice is cleared so a restart does not skip the picker. */
    fun switchModule() {
        if (!_authState.value.canSwitchModule) return
        viewModelScope.launch {
            saveActiveModuleUseCase(null)
            _authState.update { it.copy(activeModule = null) }
        }
    }

    /** [onComplete] runs once local data is cleared, so restarting the app cannot cancel the logout. */
    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            logoutUseCase()
            onComplete()
            checkAuthenticationStatus()
        }
    }

    private companion object {
        const val ROLE_REFRESH_TIMEOUT_MS = 2_500L
    }
}
