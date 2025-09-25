package com.illa.cashvan.di

import com.illa.cashvan.BuildConfig

data class SharedConfig(
    val baseUrl: String = BuildConfig.BASE_URL,
    val isDebug: Boolean = BuildConfig.DEBUG
)