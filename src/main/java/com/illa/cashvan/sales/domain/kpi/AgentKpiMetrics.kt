package com.illa.cashvan.sales.domain.kpi

import com.illa.cashvan.sales.domain.format.jsToFixed
import com.illa.cashvan.sales.domain.format.jsTrim
import java.time.Instant

// Mapping of the `agent-kpi-mobile` edge-function response to the dashboard KPI
// cards and encouragement messages, ported from the PWA's
// src/utils/agentKpiMetrics.ts. The server's percentages and ratios are shown
// as-is, never recomputed.

data class AgentKpiAgent(
    val id: String,
    val name: String?,
    val agentCode: String?,
    val phoneNumber: String?,
    val presellAgentId: String,
    val cashvanAgentId: String?,
)

data class AgentKpiPeriod(val month: Int, val year: Int, val isCurrentMonth: Boolean)

data class AgentKpiTodayVisits(val actual: Int, val target: Int, val ratio: String, val percentage: Double)

data class AgentKpiCompletionRate(val actual: Int, val expected: Int, val ratio: String, val percentage: Double)

data class AgentKpiStrikeRate(val withOrder: Int, val completed: Int, val ratio: String, val percentage: Double)

data class AgentKpiTotals(
    val presellVisits: Int,
    val cashvanVisits: Int,
    val totalVisits: Int,
    val totalOrders: Int,
    val totalOrderValue: Double,
    val totalMerchants: Int,
)

data class AgentKpiAdHocVisits(val total: Int, val withOrder: Int)

/** Response of the `agent-kpi-mobile` edge function (snake_case fields camel-cased). */
data class AgentKpiMobileResponse(
    val agent: AgentKpiAgent,
    val period: AgentKpiPeriod,
    val todayVisits: AgentKpiTodayVisits,
    val completionRate: AgentKpiCompletionRate,
    val strikeRate: AgentKpiStrikeRate,
    val totals: AgentKpiTotals,
    val adHocVisits: AgentKpiAdHocVisits,
)

enum class AgentKpiCardKey(val id: String) {
    STRIKE_RATE("strikeRate"),
    COMPLETION_RATE("completionRate"),
    TODAY_VISITS("todayVisits"),
}

/** One dashboard KPI card. Label/subtitle keys are i18n keys (see [translateKpiKey]). */
data class AgentKpiCardData(
    val key: AgentKpiCardKey,
    val labelKey: String,
    val displayValue: String,
    val percentage: Double,
    val subtitle: String? = null,
    val subtitleKey: String? = null,
    val subtitleSuffixKey: String? = null,
)

enum class AgentKpiMessageKey(val id: String) {
    LOG_VISITS("logVisits"),
    DAILY_TARGET("dailyTarget"),
}

data class AgentKpiMessageData(val key: AgentKpiMessageKey, val messageKey: String)

/** Below this completion rate (with a high strike rate) agents are nudged to log every visit. */
const val LOG_VISITS_MAX_COMPLETION_PCT = 65.0
const val LOG_VISITS_MIN_STRIKE_PCT = 80.0

/** Below this share of today's target, from [DAILY_TARGET_FROM_CAIRO_HOUR] on, the daily nudge shows. */
const val DAILY_TARGET_MAX_TODAY_PCT = 60.0
const val DAILY_TARGET_FROM_CAIRO_HOUR = 14

private const val HTTP_NOT_FOUND = 404
private const val HTTP_CONFLICT = 409
private const val MAX_KPI_QUERY_RETRIES = 3

private fun Char.isJsWhitespace(): Boolean = toString().jsTrim().isEmpty()

/** First word of the agent's name: JS `name?.trim().split(/\s+/)[0] ?? ''`. */
fun getAgentFirstName(agentName: String?): String =
    agentName?.jsTrim()?.takeWhile { !it.isJsWhitespace() } ?: ""

/** `${Number(value || 0).toFixed(1)}%`: NaN renders as "0.0%". */
private fun formatKpiPercentage(value: Double): String = "${jsToFixed(if (value.isNaN()) 0.0 else value, 1)}%"

private fun egyptHour(now: Instant): Int = now.atZone(CAIRO_ZONE).hour

fun mapAgentKpiResponseToCards(response: AgentKpiMobileResponse): List<AgentKpiCardData> = listOf(
    AgentKpiCardData(
        key = AgentKpiCardKey.STRIKE_RATE,
        labelKey = "route.strikeRate",
        displayValue = formatKpiPercentage(response.strikeRate.percentage),
        percentage = response.strikeRate.percentage,
        subtitle = response.strikeRate.ratio,
    ),
    AgentKpiCardData(
        key = AgentKpiCardKey.COMPLETION_RATE,
        labelKey = "route.completionRate",
        displayValue = formatKpiPercentage(response.completionRate.percentage),
        percentage = response.completionRate.percentage,
        subtitle = response.completionRate.ratio,
    ),
    AgentKpiCardData(
        key = AgentKpiCardKey.TODAY_VISITS,
        labelKey = "route.todayVisits",
        displayValue = response.todayVisits.ratio,
        percentage = response.todayVisits.percentage,
        subtitleKey = "route.dailyTarget",
    ),
)

/**
 * Encouragement messages, in display order. The daily-target nudge uses the
 * Cairo wall-clock hour (0–23).
 */
fun getAgentKpiMessages(response: AgentKpiMobileResponse, now: Instant = Instant.now()): List<AgentKpiMessageData> =
    buildList {
        if (
            response.completionRate.percentage < LOG_VISITS_MAX_COMPLETION_PCT &&
            response.strikeRate.percentage > LOG_VISITS_MIN_STRIKE_PCT
        ) {
            add(AgentKpiMessageData(AgentKpiMessageKey.LOG_VISITS, "kpi.messages.logVisits"))
        }
        if (
            response.todayVisits.percentage < DAILY_TARGET_MAX_TODAY_PCT &&
            egyptHour(now) >= DAILY_TARGET_FROM_CAIRO_HOUR
        ) {
            add(AgentKpiMessageData(AgentKpiMessageKey.DAILY_TARGET, "kpi.messages.dailyTarget"))
        }
    }

/** Stable identity of a message list (e.g. "logVisits,dailyTarget"); resets the banner rotation when it changes. */
fun getAgentKpiMessageKeySignature(messages: List<AgentKpiMessageData>): String =
    messages.joinToString(",") { it.key.id }

fun getAgentKpiErrorMessageKey(status: Int? = null): String = when (status) {
    HTTP_NOT_FOUND -> "kpi.errors.agentNotFound"
    HTTP_CONFLICT -> "kpi.errors.accountNotLinked"
    else -> "kpi.errors.generic"
}

/** Edge-function error status: `error.context.status ?? error.status`. */
fun getFunctionErrorStatus(contextStatus: Int?, status: Int?): Int? = contextStatus ?: status

/** 404 (agent not found) and 409 (account not linked) are permanent; anything else retries up to 3 times. */
fun shouldRetryAgentKpiQuery(failureCount: Int, status: Int?): Boolean {
    if (status == HTTP_NOT_FOUND || status == HTTP_CONFLICT) return false
    return failureCount < MAX_KPI_QUERY_RETRIES
}
