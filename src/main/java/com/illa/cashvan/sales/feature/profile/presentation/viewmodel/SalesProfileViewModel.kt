package com.illa.cashvan.sales.feature.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.supabase.SupabaseClient
import com.illa.cashvan.sales.data.CurrentAgentProvider
import com.illa.cashvan.sales.feature.kpi.data.model.AssignedRouteRef
import com.illa.cashvan.sales.feature.kpi.domain.repository.SalesKpiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import timber.log.Timber

@Serializable
data class PresellAgentProfile(
    val name: String? = null,
    val agent_code: String? = null,
    val status: Int? = null,
    val phone_number: String? = null
)

data class SalesProfileUiState(
    val isLoading: Boolean = true,
    val agent: PresellAgentProfile? = null,
    val routesLoading: Boolean = true,
    val routes: List<AssignedRouteRef> = emptyList()
)

/** The PWA's presell `AgentProfile`: the Supabase agent and their assigned routes. */
class SalesProfileViewModel(
    private val supabaseClient: SupabaseClient,
    private val currentAgentProvider: CurrentAgentProvider,
    private val kpiRepository: SalesKpiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesProfileUiState())
    val uiState: StateFlow<SalesProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val agent = runCatching {
                val id = currentAgentProvider.require().agentId
                supabaseClient.selectFirstOrNull<PresellAgentProfile>("agents") {
                    select("name,agent_code,status,phone_number")
                    eq("id", id)
                }
            }.onFailure { Timber.e(it, "Profile load failed") }.getOrNull()
            _uiState.update { it.copy(isLoading = false, agent = agent) }
        }
        viewModelScope.launch {
            val routes = runCatching { kpiRepository.getAssignedRoutes() }.getOrDefault(emptyList())
            _uiState.update { it.copy(routesLoading = false, routes = routes) }
        }
    }
}
