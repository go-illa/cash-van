package com.illa.cashvan.core.analytics

import timber.log.Timber
import java.util.Collections

class CashVanAnalyticsManager : CashVanAnalyticsHelper {
    private val tag: String = this::class.java.simpleName

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
                Timber
                    .tag(tag)
                    .e(e, "${listener::class.java.simpleName} failed To identify user")
            }
        }
    }

    override fun logErrorEvent(event: String, params: Map<String, String>) {
        listeners.forEach { listener ->
            try {
                listener.logErrorEvent(event, params)
            } catch (e: Exception) {
                Timber
                    .tag(tag)
                    .e(e, "${listener::class.java.simpleName} failed To log Error Event :$event")
            }
        }
    }

    override fun logEvent(event: String, params: Map<String, String>) {
        listeners.forEach { listener ->
            try {
                listener.logEvent(event, params)
            } catch (e: Exception) {
                Timber
                    .tag(tag)
                    .e(e, "${listener::class.java.simpleName} failed To log Event :$event")
            }
        }
    }
}
