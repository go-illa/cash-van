package com.illa.cashvan.core.analytics

import android.content.Context
import com.illa.cashvan.BuildConfig
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * Mixpanel for the Sales module, following the sales PWA's `mixpanel_events.md`: snake_case names and
 * properties, identified by agent code, reset on logout. Every event carries `platform` so native and PWA
 * traffic can be told apart in the shared project. Analytics never throws.
 */
class SalesAnalytics(context: Context) {

    private val mixpanel: MixpanelAPI? = runCatching {
        MixpanelAPI.getInstance(context, BuildConfig.SALES_MIXPANEL_TOKEN, false, INSTANCE_NAME, false).apply {
            setServerURL(EU_HOST)
            setEnableLogging(BuildConfig.DEBUG)
            registerBaseProperties()
        }
    }.onFailure { Timber.w(it, "Sales Mixpanel init failed") }.getOrNull()

    fun identify(agentCode: String, agentId: String, agentName: String?) = safely {
        identify(agentCode)
        people.set(JSONObject().put("\$name", agentName).put("agent_id", agentId).put("agent_code", agentCode))
        registerSuperProperties(JSONObject().put("agent_id", agentId).put("agent_code", agentCode).put("agent_name", agentName))
    }

    fun track(event: String, vararg properties: Pair<String, Any?>) = safely {
        track(event, JSONObject().apply { properties.forEach { (key, value) -> put(key, value.toJson()) } })
    }

    /** On logout, so the next agent on the device starts a fresh identity. */
    fun reset() = safely {
        reset()
        registerBaseProperties()
    }

    private fun MixpanelAPI.registerBaseProperties() {
        registerSuperProperties(JSONObject().put("app_version", BuildConfig.VERSION_NAME).put("platform", PLATFORM))
    }

    private inline fun safely(block: MixpanelAPI.() -> Unit) {
        val api = mixpanel ?: return
        try {
            api.block()
        } catch (e: Exception) {
            Timber.w(e, "Sales analytics call failed")
        }
    }

    private fun Any?.toJson(): Any? = when (this) {
        null -> JSONObject.NULL
        is Map<*, *> -> JSONObject().also { json -> forEach { (key, value) -> json.put(key.toString(), value.toJson()) } }
        is Collection<*> -> JSONArray().also { array -> forEach { array.put(it.toJson()) } }
        is Enum<*> -> name.lowercase()
        else -> this
    }

    private companion object {
        const val INSTANCE_NAME = "sales"
        const val EU_HOST = "https://api-eu.mixpanel.com"
        const val PLATFORM = "android_native"
    }
}
