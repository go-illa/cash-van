package com.illa.cashvan.core.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkConnectivityService(
    private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _connectivityStatus = MutableStateFlow<ConnectivityStatus>(ConnectivityStatus.Unknown)
    val connectivityStatus: StateFlow<ConnectivityStatus> = _connectivityStatus.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _connectivityStatus.value = ConnectivityStatus.Connected
        }

        override fun onLost(network: Network) {
            _connectivityStatus.value = ConnectivityStatus.Disconnected
        }

        override fun onUnavailable() {
            _connectivityStatus.value = ConnectivityStatus.Disconnected
        }
    }

    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)

            // Set initial state
            _connectivityStatus.value = if (isConnected()) {
                ConnectivityStatus.Connected
            } else {
                ConnectivityStatus.Disconnected
            }
        } catch (e: Exception) {
            // Handle registration failure
            _connectivityStatus.value = ConnectivityStatus.Unknown
        }
    }

    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Callback was not registered or already unregistered
        }
    }

    suspend fun refreshConnectivity() {
        _connectivityStatus.value = if (isConnected()) {
            ConnectivityStatus.Connected
        } else {
            ConnectivityStatus.Disconnected
        }
    }

    private fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
