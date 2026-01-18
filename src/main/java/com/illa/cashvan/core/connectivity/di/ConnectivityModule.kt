package com.illa.cashvan.core.connectivity.di

import com.illa.cashvan.core.connectivity.ConnectivityViewModel
import com.illa.cashvan.core.connectivity.NetworkConnectivityService
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val connectivityModule = module {
    single { NetworkConnectivityService(get()) }
    viewModel { ConnectivityViewModel(get()) }
}
