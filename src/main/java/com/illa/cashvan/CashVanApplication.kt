package com.illa.cashvan

import android.app.Application
import com.illa.cashvan.core.connectivity.NetworkConnectivityService
import com.illa.cashvan.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.android.ext.android.inject

class CashVanApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@CashVanApplication)
            modules(appModule)
        }

        // Start connectivity monitoring
        val connectivityService: NetworkConnectivityService by inject()
        connectivityService.startMonitoring()
    }
}