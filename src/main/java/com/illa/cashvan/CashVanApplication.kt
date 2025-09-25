package com.illa.cashvan

import android.app.Application
import com.illa.cashvan.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class CashVanApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@CashVanApplication)
            modules(appModule)
        }
    }
}