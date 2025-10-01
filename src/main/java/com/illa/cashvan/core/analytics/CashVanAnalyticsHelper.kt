package com.illa.cashvan.core.analytics

import timber.log.Timber

interface CashVanAnalyticsHelper {
    fun identify(phoneNumber: String)
    fun logErrorEvent(event: String, params: Map<String, String> = emptyMap())
    fun logEvent(event: String, params: Map<String, String> = emptyMap())
}

object CashVanAnalyticsHelperPreview : CashVanAnalyticsHelper {
    override fun identify(phoneNumber: String) {
        Timber.tag("AnalyticsHelperPreview").d("identify")
    }

    override fun logErrorEvent(event: String, params: Map<String, String>) {
        Timber.tag("AnalyticsHelperPreview").d("Event: $event with params: $params")
    }

    override fun logEvent(event: String, params: Map<String, String>) {
        Timber.tag("AnalyticsHelperPreview").d("Event: $event with params: $params")
    }
}

fun CashVanAnalyticsHelper.logScreenView(screenName: String, screenParams: Map<String, String> = emptyMap()) {
    logEvent(screenName, screenParams)
}
