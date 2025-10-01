package com.illa.cashvan.core.analytics

import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject

class CashVanMixPanelAnalytics(private val mixpanel: MixpanelAPI) : CashVanAnalyticsHelper {
    override fun identify(phoneNumber: String) {
        mixpanel.identify(phoneNumber)
        mixpanel.deviceInfo
        mixpanel.registerSuperProperties(createUserProperties(phoneNumber))
    }

    override fun logErrorEvent(event: String, params: Map<String, String>) {
        mixpanel.track(event, JSONObject(params))
    }

    override fun logEvent(event: String, params: Map<String, String>) {
        mixpanel.track(event, JSONObject(params))
    }

    private fun createUserProperties(phoneNumber: String): JSONObject {
        return JSONObject().apply {
            put("PhoneNumber", phoneNumber)
        }
    }
}
