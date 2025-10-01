package com.illa.cashvan.core.analytics.di

import com.illa.cashvan.BuildConfig
import com.illa.cashvan.core.analytics.CashVanAnalyticsHelper
import com.illa.cashvan.core.analytics.CashVanAnalyticsManager
import com.illa.cashvan.core.analytics.CashVanMixPanelAnalytics
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val analyticsModule = module {
    single {
        val mixpanel = MixpanelAPI.getInstance(androidContext(), BuildConfig.MIXPANEL_TOKEN, false)
        mixpanel.setEnableLogging(BuildConfig.DEBUG)
        CashVanMixPanelAnalytics(mixpanel)
    }

    single {
        CashVanAnalyticsManager().apply {
            addListener(get<CashVanMixPanelAnalytics>())
        }
    }

    single<CashVanAnalyticsHelper> { get<CashVanAnalyticsManager>() }
}
