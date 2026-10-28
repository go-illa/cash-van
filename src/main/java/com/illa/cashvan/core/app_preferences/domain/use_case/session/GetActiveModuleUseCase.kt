package com.illa.cashvan.core.app_preferences.domain.use_case.session

import com.illa.cashvan.core.app_preferences.domain.repository.PreferencesRepo
import com.illa.cashvan.core.app_preferences.domain.use_case.session.SaveActiveModuleUseCase.Companion.KEY_ACTIVE_MODULE
import com.illa.cashvan.core.session.AgentRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetActiveModuleUseCase(
    private val preferencesRepo: PreferencesRepo
) {
    suspend operator fun invoke(): Flow<AgentRole?> {
        return preferencesRepo.getValue(KEY_ACTIVE_MODULE, "").map { AgentRole.fromApiValue(it) }
    }
}
