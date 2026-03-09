package com.illa.cashvan.core.analytics

interface CashVanAnalyticsHelper {
    fun identify(phoneNumber: String)
    fun logErrorEvent(event: String, params: Map<String, String> = emptyMap())
    fun logEvent(event: String, params: Map<String, String> = emptyMap())
}

object CashVanAnalyticsHelperPreview : CashVanAnalyticsHelper {
    override fun identify(phoneNumber: String) {}

    override fun logErrorEvent(event: String, params: Map<String, String>) {}

    override fun logEvent(event: String, params: Map<String, String>) {}
}

fun CashVanAnalyticsHelper.logScreenView(screenName: String, screenParams: Map<String, String> = emptyMap()) {
    logEvent(screenName, screenParams)
}
