package com.illa.cashvan.core.app_preferences.domain.use_case.session

import com.illa.cashvan.core.app_preferences.domain.repository.PreferencesRepo
import com.illa.cashvan.core.session.AgentRole

class SaveActiveModuleUseCase(
    private val preferencesRepo: PreferencesRepo
) {
    /** Passing `null` forgets the last choice, so an agent with both roles sees the module picker. */
    suspend operator fun invoke(module: AgentRole?) {
        if (module == null) {
            preferencesRepo.clearPreference(KEY_ACTIVE_MODULE)
        } else {
            preferencesRepo.setValue(KEY_ACTIVE_MODULE, module.apiValue)
        }
    }

    companion object {
        internal const val KEY_ACTIVE_MODULE = "active_module"
    }
}
