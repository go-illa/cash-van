package com.illa.cashvan.sales.domain.kpi

import com.illa.cashvan.sales.domain.format.formatPercent
import com.illa.cashvan.sales.domain.format.jsToFixed

// Display logic of the PWA's dashboard KPI section: AgentKpiSection.tsx
// (coverage card override, encouragement banner), KPICards.tsx (subtitle,
// colour thresholds, ring) and useAgentCoverage.ts (coverage from the KPI summary).

/**
 * Arabic copy of the i18n keys used by the KPI cards, messages and errors
 * (public/locales/ar/translation.json; the PWA ships Arabic only).
 */
val KPI_ARABIC_TRANSLATIONS: Map<String, String> = mapOf(
    "route.strikeRate" to "الزيارات الناجحة",
    "route.completionRate" to "معدل الزيارات الشهري",
    "route.completionRateCoverage" to "التغطية",
    "route.todayVisits" to "زيارات اليوم",
    "route.dailyTarget" to "هدف يومي",
    "kpi.messages.logVisits" to "شغلك في البيع ممتاز، بس محتاجين نسجل كل زيارة حتى لو التاجر مش عايز يطلب.",
    "kpi.messages.dailyTarget" to "لسه قدامنا فرصة نلحق تارجت النهارده. يلا نحقق هدف النهارده زيارة ورا زيارة.",
    "kpi.errors.agentNotFound" to "المؤشرات غير متاحة. لم يتم العثور على المندوب، تواصل مع المشرف.",
    "kpi.errors.accountNotLinked" to "المؤشرات غير متاحة. حسابك غير مربوط بالكامل، تواصل مع الإدارة.",
    "kpi.errors.generic" to "المؤشرات غير متاحة الآن. حدّث وحاول مرة أخرى.",
    "retry" to "حاول تاني",
)

private const val KPI_GREETING_TEMPLATE = "يا {{firstName}}، "

/** "See more" affordance shown on clickable KPI cards. */
const val KPI_CARD_SEE_MORE_LABEL = "شاهد المزيد"

/** The encouragement banner rotates through active messages at this interval. */
const val KPI_MESSAGE_ROTATION_MS = 9_000L

/** i18next `t(key)`: a key without a translation renders as the key itself. */
fun translateKpiKey(key: String): String = KPI_ARABIC_TRANSLATIONS[key] ?: key

/** The logged-in agent's real coverage for a month (from `get_kpi_agents_summary`). */
data class AgentCoverage(
    val uniqueVisited: Int,
    val totalMerchants: Int,
    /** unique ÷ assigned × 100, capped at 100. */
    val percentage: Double,
)

/** Coverage of [agentId] from the summary rows; zeros when the agent has no row. */
fun computeAgentCoverage(summaryRows: List<KpiAgentSummaryRow>, agentId: String): AgentCoverage {
    val mySummary = summaryRows.firstOrNull { it.agentId == agentId }
    val uniqueVisited = mySummary?.uniqueVisits ?: 0
    val totalMerchants = mySummary?.totalMerchants ?: 0
    val percentage = if (totalMerchants > 0) minOf(uniqueVisited.toDouble() / totalMerchants * 100, 100.0) else 0.0
    return AgentCoverage(uniqueVisited, totalMerchants, percentage)
}

/**
 * Replaces the middle "completionRate" card (labelled monthly visit rate) with
 * real coverage: visited/assigned merchants. Cards are unchanged until coverage loads.
 */
fun applyAgentCoverage(
    cards: List<AgentKpiCardData>,
    coverage: AgentCoverage?,
    translate: (String) -> String = ::translateKpiKey,
): List<AgentKpiCardData> {
    if (coverage == null) return cards
    return cards.map { card ->
        if (card.key != AgentKpiCardKey.COMPLETION_RATE) return@map card
        card.copy(
            labelKey = "route.completionRateCoverage",
            displayValue = "${coverage.uniqueVisited}/${coverage.totalMerchants}",
            percentage = coverage.percentage,
            subtitle = "${formatPercent(coverage.percentage)} ${translate("route.completionRateCoverage")}",
        )
    }
}

enum class KpiDestination(val pwaPath: String) {
    VISITS("/kpi/visits"),
    COVERAGE("/kpi/coverage"),
    CONVERSION("/kpi/conversion"),
}

/** Detail page opened by tapping a KPI card. */
fun kpiCardDestination(key: AgentKpiCardKey): KpiDestination = when (key) {
    AgentKpiCardKey.TODAY_VISITS -> KpiDestination.VISITS
    AgentKpiCardKey.COMPLETION_RATE -> KpiDestination.COVERAGE
    AgentKpiCardKey.STRIKE_RATE -> KpiDestination.CONVERSION
}

/** KPI card status colour: danger below 50%, progress below 75%, success otherwise. */
enum class KpiStatusTone { DANGER, PROGRESS, SUCCESS }

fun kpiStatusTone(percentage: Double): KpiStatusTone = when {
    percentage < 50 -> KpiStatusTone.DANGER
    percentage < 75 -> KpiStatusTone.PROGRESS
    else -> KpiStatusTone.SUCCESS
}

/** Card subtitle: the translated subtitle key, else the raw ratio, plus an optional translated suffix. */
fun resolveKpiCardSubtitle(card: AgentKpiCardData, translate: (String) -> String = ::translateKpiKey): String {
    val baseSubtitle = card.subtitleKey?.let(translate) ?: card.subtitle ?: ""
    return card.subtitleSuffixKey?.let { "$baseSubtitle ${translate(it)}" } ?: baseSubtitle
}

/** Text inside the progress ring once the count-up settles: `${value.toFixed(0)}%`. */
fun kpiRingLabel(percentage: Double): String = "${jsToFixed(percentage, 0)}%"

/** Filled share of the ring (0–1); only the top is clamped, as in the PWA. */
fun kpiRingFraction(percentage: Double): Double = minOf(percentage, 100.0) / 100

/** Greeting before an encouragement message ("يا Ahmed، "), or null without a usable first name. */
fun kpiEncouragementGreeting(agentName: String?): String? {
    val firstName = getAgentFirstName(agentName)
    if (firstName.isEmpty()) return null
    return KPI_GREETING_TEMPLATE.replace("{{firstName}}", firstName)
}

/** Whether the banner rotates (only with two or more messages). */
fun shouldRotateKpiMessages(messageCount: Int): Boolean = messageCount >= 2

/** Next banner index; the index resets to 0 whenever [getAgentKpiMessageKeySignature] changes. */
fun nextKpiMessageIndex(currentIndex: Int, messageCount: Int): Int = (currentIndex + 1) % messageCount

data class KpiEncouragementBanner(
    val key: AgentKpiMessageKey,
    /** Rendered isolated (bdi) before [message]; null when the agent has no first name. */
    val greeting: String?,
    val message: String,
)

/** The banner for the active message, or null (banner hidden) when no messages are active. */
fun kpiEncouragementBanner(
    messages: List<AgentKpiMessageData>,
    activeIndex: Int,
    agentName: String?,
    translate: (String) -> String = ::translateKpiKey,
): KpiEncouragementBanner? {
    val active = messages.getOrNull(activeIndex) ?: messages.firstOrNull() ?: return null
    return KpiEncouragementBanner(
        key = active.key,
        greeting = kpiEncouragementGreeting(agentName),
        message = translate(active.messageKey),
    )
}
