package com.illa.cashvan.core.notifications.di

import com.illa.cashvan.core.notifications.FcmTokenProvider
import org.koin.dsl.module

val notificationsModule = module {
    single { FcmTokenProvider() }
}
