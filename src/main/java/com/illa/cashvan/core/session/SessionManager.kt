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
}
