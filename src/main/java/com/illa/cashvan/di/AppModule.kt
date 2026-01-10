package com.illa.cashvan.di

import com.illa.cashvan.core.analytics.di.analyticsModule
import com.illa.cashvan.core.app_preferences.di.preferencesModule
import com.illa.cashvan.core.connectivity.di.connectivityModule
import com.illa.cashvan.core.location.di.locationModule
import com.illa.cashvan.core.network.di.networkModule
import com.illa.cashvan.feature.auth.di.authModule
import com.illa.cashvan.feature.merchant.di.merchantModule
import com.illa.cashvan.feature.orders.di.orderModule
import com.illa.cashvan.feature.plans.di.plansModule
import com.illa.cashvan.feature.profile.di.profileModule
import org.koin.dsl.module

val appModule = module {
    includes(preferencesModule, networkModule, connectivityModule, analyticsModule, authModule, profileModule, locationModule, merchantModule, plansModule, orderModule)
}