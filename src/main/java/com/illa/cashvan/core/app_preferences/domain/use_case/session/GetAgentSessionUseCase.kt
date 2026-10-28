package com.illa.cashvan.core.app_preferences.domain.use_case.session

import com.illa.cashvan.core.app_preferences.domain.repository.PreferencesRepo
import com.illa.cashvan.core.app_preferences.domain.use_case.session.SaveAgentSessionUseCase.Companion.KEY_AGENT_SESSION
import com.illa.cashvan.core.session.AgentSession
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.serializer

class GetAgentSessionUseCase(
    private val preferencesRepo: PreferencesRepo
) {
    suspend operator fun invoke(): Flow<AgentSession?> {
        return preferencesRepo.getValue(
            key = KEY_AGENT_SESSION,
            defaultValue = null,
            serializer = serializer<AgentSession?>(),
            decrypt = true
        )
    }
}
