package com.illa.cashvan.di

import com.illa.cashvan.core.app_preferences.di.preferencesModule
import com.illa.cashvan.core.location.di.locationModule
import com.illa.cashvan.core.network.di.networkModule
import com.illa.cashvan.feature.auth.di.authModule
import com.illa.cashvan.feature.profile.di.profileModule
import org.koin.dsl.module

val appModule = module {
    includes(preferencesModule, networkModule, authModule, profileModule, locationModule)
}