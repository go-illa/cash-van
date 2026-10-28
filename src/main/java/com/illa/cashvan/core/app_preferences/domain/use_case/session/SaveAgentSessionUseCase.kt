package com.illa.cashvan.core.app_preferences.domain.use_case.session

import com.illa.cashvan.core.app_preferences.domain.repository.PreferencesRepo
import com.illa.cashvan.core.session.AgentSession
import kotlinx.serialization.serializer

class SaveAgentSessionUseCase(
    private val preferencesRepo: PreferencesRepo
) {
    suspend operator fun invoke(session: AgentSession) {
        preferencesRepo.setValue(
            key = KEY_AGENT_SESSION,
            value = session,
            serializer = serializer(),
            encrypt = true
        )
    }

    companion object {
        internal const val KEY_AGENT_SESSION = "agent_session"
    }
}
