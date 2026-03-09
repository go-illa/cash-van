package com.illa.cashvan.core.analytics

import java.util.Collections

class CashVanAnalyticsManager : CashVanAnalyticsHelper {
    private val listeners: MutableSet<CashVanAnalyticsHelper> = Collections.synchronizedSet(mutableSetOf())

    fun addListener(listener: CashVanAnalyticsHelper) {
        listeners.add(listener)
    }

    @Suppress("unused")
    fun removeListener(listener: CashVanAnalyticsHelper) {
        listeners.remove(listener)
    }

    override fun identify(phoneNumber: String) {
        listeners.forEach { listener ->
            try {
                listener.identify(phoneNumber)
            } catch (e: Exception) {
            }
        }
    }

    override fun logErrorEvent(event: String, params: Map<String, String>) {
        listeners.forEach { listener ->
            try {
                listener.logErrorEvent(event, params)
            } catch (e: Exception) {
            }
        }
    }

    override fun logEvent(event: String, params: Map<String, String>) {
        listeners.forEach { listener ->
            try {
                listener.logEvent(event, params)
            } catch (e: Exception) {
            }
        }
    }
}
