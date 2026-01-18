package com.illa.cashvan.core.connectivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectivityViewModel(
    private val networkConnectivityService: NetworkConnectivityService
) : ViewModel() {

    val connectivityStatus: StateFlow<ConnectivityStatus> =
        networkConnectivityService.connectivityStatus

    private val _isRetrying = MutableStateFlow(false)
    val isRetrying: StateFlow<Boolean> = _isRetrying.asStateFlow()

    fun checkConnectivity() {
        viewModelScope.launch {
            _isRetrying.value = true
            // Minimum visual feedback duration
            delay(300)
            networkConnectivityService.refreshConnectivity()
            _isRetrying.value = false
        }
    }
}
