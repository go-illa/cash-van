package com.illa.cashvan.feature.auth.domain.usecase

import com.illa.cashvan.core.app_preferences.domain.use_case.session.GetAgentSessionUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.session.SaveAgentSessionUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.user.GetUserUseCase
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.core.session.AgentRole
import com.illa.cashvan.core.session.AgentSession
import com.illa.cashvan.shared.feature.profile.domain.usecase.GetProfileUseCase
import kotlinx.coroutines.flow.firstOrNull

/**
 * Picks up role changes without a new login. Does nothing until the profile endpoint returns `roles`.
 */
class RefreshAgentRolesUseCase(
    private val getUserUseCase: GetUserUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getAgentSessionUseCase: GetAgentSessionUseCase,
    private val saveAgentSessionUseCase: SaveAgentSessionUseCase
) {
    suspend operator fun invoke() {
        // Roles come from the Rails profile; presell-only agents have no Rails account to ask.
        if (getAgentSessionUseCase().firstOrNull()?.hasRailsAccess == false) return
        val user = getUserUseCase().firstOrNull() ?: return
        val profile = (getProfileUseCase(user.id) as? ApiResult.Success)?.data ?: return
        val roles = profile.roles ?: return

        val current = getAgentSessionUseCase().firstOrNull() ?: AgentSession(salesAgentId = user.id)
        val agentId = profile.agent_id ?: current.agentId
        // A sales role without a Supabase agent id would open a Sales module that cannot load anything.
        if (AgentRole.SALES in AgentRole.parse(roles) && agentId == null) return

        saveAgentSessionUseCase(current.copy(roles = roles, agentId = agentId))
    }
}
