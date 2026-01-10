package com.illa.cashvan.core.connectivity

sealed class ConnectivityStatus {
    data object Connected : ConnectivityStatus()
    data object Disconnected : ConnectivityStatus()
    data object Unknown : ConnectivityStatus()
}
