package com.illa.cashvan.core.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SessionManager {
    private val _forceLogoutEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val forceLogoutEvents: SharedFlow<Unit> = _forceLogoutEvents.asSharedFlow()

    fun triggerForceLogout() {
        _forceLogoutEvents.tryEmit(Unit)
    }

    private val _inactiveAgentEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val inactiveAgentEvents: SharedFlow<Unit> = _inactiveAgentEvents.asSharedFlow()

    fun triggerInactiveAgent() {
        _inactiveAgentEvents.tryEmit(Unit)
    }
}
