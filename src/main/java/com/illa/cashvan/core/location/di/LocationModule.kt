package com.illa.cashvan.core.location.di

import com.illa.cashvan.core.location.LocationService
import com.illa.cashvan.core.location.LocationViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val locationModule = module {
    single { LocationService(androidContext()) }
    viewModel { LocationViewModel(get()) }
}