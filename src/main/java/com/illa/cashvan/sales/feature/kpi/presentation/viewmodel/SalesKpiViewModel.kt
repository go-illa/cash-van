package com.illa.cashvan.sales.feature.kpi.presentation.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.analytics.SalesAnalytics
import com.illa.cashvan.core.app_preferences.domain.repository.PreferencesRepo
import com.illa.cashvan.sales.domain.kpi.AgentCommission
import com.illa.cashvan.sales.domain.kpi.AgentCoverage
import com.illa.cashvan.sales.domain.kpi.AgentKpiCardData
import com.illa.cashvan.sales.domain.kpi.AgentKpiMobileResponse
import com.illa.cashvan.sales.domain.kpi.CommissionProgressView
import com.illa.cashvan.sales.domain.kpi.KpiComparisonScope
import com.illa.cashvan.sales.domain.kpi.KpiPeriod
import com.illa.cashvan.sales.domain.kpi.WorkWeek
import com.illa.cashvan.sales.domain.kpi.applyAgentCoverage
import com.illa.cashvan.sales.domain.kpi.buildCommissionProgressView
import com.illa.cashvan.sales.domain.kpi.cairoWeekday
import com.illa.cashvan.sales.domain.kpi.commissionUnlockStorageKey
import com.illa.cashvan.sales.domain.kpi.computeAgentCoverage
import com.illa.cashvan.sales.domain.kpi.currentCairoPeriod
import com.illa.cashvan.sales.domain.kpi.currentCommissionMonthKey
import com.illa.cashvan.sales.domain.kpi.currentMonthDateISO
import com.illa.cashvan.sales.domain.kpi.mapAgentKpiResponseToCards
import com.illa.cashvan.sales.domain.kpi.monthBounds
import com.illa.cashvan.sales.domain.kpi.parseCommissionUnlock
import com.illa.cashvan.sales.domain.kpi.planUnlockCelebration
import com.illa.cashvan.sales.domain.kpi.reduceCommissionRows
import com.illa.cashvan.sales.domain.kpi.resolveKpiComparisonScope
import com.illa.cashvan.sales.domain.kpi.serializeCommissionUnlock
import com.illa.cashvan.sales.domain.kpi.workWeekFor
import com.illa.cashvan.sales.data.parseTimestamp
import com.illa.cashvan.sales.feature.kpi.data.mapper.toDomain
import com.illa.cashvan.sales.feature.kpi.data.model.AssignedRouteRef
import com.illa.cashvan.sales.feature.kpi.data.model.RoutePeriodMetricsRow
import com.illa.cashvan.sales.feature.kpi.domain.repository.KpiUnavailableException
import com.illa.cashvan.sales.feature.kpi.domain.repository.SalesKpiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import kotlin.math.roundToInt

data class KpiHubState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val kpi: AgentKpiMobileResponse? = null,
    val cards: List<AgentKpiCardData> = emptyList(),
    val commission: CommissionProgressView? = null,
    /** Set when the commission unlocked this month for the first time; the hub celebrates once. */
    val celebrateUnlock: Boolean = false
)

data class VisitsWeekState(
    val isLoading: Boolean = true,
    val week: WorkWeek? = null,
    /** Visits per day, Saturday to Thursday. */
    val dailyCounts: List<Int> = emptyList(),
    val dailyTarget: Int = DEFAULT_DAILY_TARGET
) {
    val total: Int get() = dailyCounts.sum()
    val weeklyTarget: Int get() = dailyTarget * WORK_DAYS
}

data class CoverageState(
    val isLoading: Boolean = true,
    val routes: List<AssignedRouteRef> = emptyList(),
    val routeId: String? = null,
    val month: RoutePeriodMetricsRow? = null,
    val scope: KpiComparisonScope? = null,
    val primary: RoutePeriodMetricsRow? = null,
    val comparison: RoutePeriodMetricsRow? = null
)

data class ConversionState(
    val isLoading: Boolean = true,
    val kpi: AgentKpiMobileResponse? = null,
    val scope: KpiComparisonScope? = null,
    val primary: RoutePeriodMetricsRow? = null,
    val comparison: RoutePeriodMetricsRow? = null,
    val topReason: String? = null
)

data class ReportState(
    val isLoading: Boolean = true,
    val kpi: AgentKpiMobileResponse? = null,
    val coverage: AgentCoverage? = null
)

const val DEFAULT_DAILY_TARGET = 25
const val WORK_DAYS = 6

/** The PWA's KPI staleTime and the commission card's refetch interval. */
const val HUB_STALE_MS = 60_000L
const val COMMISSION_REFRESH_MS = 60_000L

/**
 * The Sales KPI hub and its four detail pages (visits, coverage, conversion, report), as the PWA's
 * `/kpi/...` pages. Periods are Cairo calendar months; the work week runs Saturday to Thursday.
 */
class SalesKpiViewModel(
    private val repository: SalesKpiRepository,
    private val preferencesRepo: PreferencesRepo,
    private val analytics: SalesAnalytics
) : ViewModel() {

    private val _hub = MutableStateFlow(KpiHubState())
    val hub: StateFlow<KpiHubState> = _hub.asStateFlow()

    private val _period = MutableStateFlow(currentCairoPeriod())
    val period: StateFlow<KpiPeriod> = _period.asStateFlow()

    private val _visits = MutableStateFlow(VisitsWeekState())
    val visits: StateFlow<VisitsWeekState> = _visits.asStateFlow()

    private val _coverage = MutableStateFlow(CoverageState())
    val coverage: StateFlow<CoverageState> = _coverage.asStateFlow()

    private val _conversion = MutableStateFlow(ConversionState())
    val conversion: StateFlow<ConversionState> = _conversion.asStateFlow()

    private val _report = MutableStateFlow(ReportState())
    val report: StateFlow<ReportState> = _report.asStateFlow()

    private var weekReference: Instant = Instant.now()

    private var hubJob: Job? = null
    private var hubLoadedAt = 0L
    private var lastCommission: AgentCommission? = null

    init {
        loadHub()
    }

    fun loadHub() = loadHub(silent = false)

    /**
     * Opening the hub or coming back to the app refetches once the data is a minute old, as the
     * PWA's queries do (staleTime 60 s). Content already on screen stays while it refreshes.
     */
    fun refreshHubIfStale(nowMs: Long = SystemClock.elapsedRealtime()) {
        if (hubJob?.isActive == true) return
        val state = _hub.value
        if (state.error == null && nowMs - hubLoadedAt < HUB_STALE_MS) return
        loadHub(silent = state.error == null && state.kpi != null)
    }

    /** The commission card is live: the PWA refetches it every minute while the hub is open. */
    fun refreshCommission() {
        if (hubJob?.isActive == true || _hub.value.kpi == null) return
        viewModelScope.launch {
            val (view, celebrate) = runCatching { loadCommission() }.getOrNull() ?: return@launch
            _hub.update { it.copy(commission = view, celebrateUnlock = it.celebrateUnlock || celebrate) }
        }
    }

    private fun loadHub(silent: Boolean) {
        hubJob?.cancel()
        hubJob = viewModelScope.launch {
            if (!silent) _hub.update { it.copy(isLoading = true, error = null) }
            try {
                val now = currentCairoPeriod()
                val kpi = async { repository.getAgentKpi().toDomain() }
                val coverage = async { runCatching { loadCoverageFor(now) }.getOrNull() }
                val commission = async { runCatching { loadCommission() }.getOrNull() }
                val response = kpi.await()
                val cards = applyAgentCoverage(mapAgentKpiResponseToCards(response), coverage.await())
                val (view, celebrate) = commission.await() ?: (null to false)
                hubLoadedAt = SystemClock.elapsedRealtime()
                _hub.value = KpiHubState(isLoading = false, kpi = response, cards = cards, commission = view, celebrateUnlock = celebrate)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "KPI hub failed")
                // A failed background refresh keeps what is on screen.
                if (!silent) {
                    val message = (e as? KpiUnavailableException)?.userMessage ?: "المؤشرات غير متاحة الآن. حدّث وحاول مرة أخرى."
                    _hub.value = KpiHubState(isLoading = false, error = message)
                }
            }
        }
    }

    /** KPI hub clicks, named as in `mixpanel_events.md`. */
    fun trackHubClick(event: String, destination: String) {
        analytics.track(event, "kpi_page" to "hub", "destination" to destination)
    }

    fun retryHub() {
        analytics.track("kpi_retry_clicked", "kpi_page" to "hub")
        loadHub()
    }

    fun unlockCelebrated() {
        _hub.update { it.copy(celebrateUnlock = false) }
    }

    private suspend fun loadCoverageFor(period: KpiPeriod): AgentCoverage {
        val rows = repository.getAgentsSummary(period.month, period.year).map { it.toDomain() }
        return computeAgentCoverage(rows, repository.currentAgentId())
    }

    /** The commission card, and whether this is the first time it unlocked this month (a sticky latch). */
    private suspend fun loadCommission(): Pair<CommissionProgressView?, Boolean> {
        val rows = repository.getCommissionRows(currentMonthDateISO()) ?: return null to false
        val commission = reduceCommissionRows(rows.map { it.toDomain() }, currentCommissionMonthKey())
            ?: return null to false
        val key = commissionUnlockStorageKey(repository.currentAgentId())
        val stored = parseCommissionUnlock(preferencesRepo.getValue(key, "").first().ifEmpty { null })
        val plan = planUnlockCelebration(stored, commission)
        plan.latchToWrite?.let { latch -> serializeCommissionUnlock(latch)?.let { preferencesRepo.setValue(key, it) } }
        lastCommission = commission
        if (plan.celebrate) {
            analytics.track(
                "commission_unlock_celebration_shown",
                "payout_egp" to commission.payoutEgp.roundToInt(), "achievement_pct" to commission.achievementPct.roundToInt(), "month" to commission.monthKey
            )
        }
        return buildCommissionProgressView(commission) to plan.celebrate
    }

    /** Once per hub visit and per month/unlock state, so the minute refreshes don't repeat it (as the PWA). */
    fun trackCommissionViewed() {
        val commission = lastCommission ?: return
        analytics.track(
            "commission_progress_card_viewed",
            "achievement_pct" to commission.achievementPct.roundToInt(), "unlocked" to commission.unlocked, "month" to commission.monthKey
        )
    }

    fun selectPeriod(period: KpiPeriod) {
        _period.value = period
    }

    /** Detail pages always open on the current Cairo month, as the PWA's month picker resets. */
    fun resetPeriod() {
        _period.value = currentCairoPeriod()
    }

    fun loadVisits(reference: Instant = weekReference) {
        weekReference = reference
        viewModelScope.launch {
            _visits.update { it.copy(isLoading = true) }
            val week = workWeekFor(reference)
            val dailyTarget = _hub.value.kpi?.todayVisits?.target?.takeIf { it > 0 } ?: DEFAULT_DAILY_TARGET
            val counts = MutableList(WORK_DAYS) { 0 }
            runCatching { repository.getVisits(week.start, week.end) }
                .onFailure { Timber.e(it, "Week visits failed") }
                .getOrDefault(emptyList())
                .filter { it.is_ad_hoc != true }
                .forEach { visit ->
                    val instant = parseTimestamp(visit.visit_date) ?: return@forEach
                    // Cairo weekday 6 is Saturday; Saturday to Thursday become 0..5.
                    val index = (cairoWeekday(instant) + 1) % 7
                    if (index < WORK_DAYS) counts[index]++
                }
            _visits.value = VisitsWeekState(isLoading = false, week = week, dailyCounts = counts, dailyTarget = dailyTarget)
        }
    }

    fun shiftWeek(weeks: Long) {
        loadVisits(weekReference.plusSeconds(weeks * 7 * 24 * 3600))
    }

    fun loadCoverage(routeId: String? = _coverage.value.routeId) {
        viewModelScope.launch {
            _coverage.update { it.copy(isLoading = true, routeId = routeId) }
            val period = _period.value
            val bounds = monthBounds(period.month, period.year)
            val scope = resolveKpiComparisonScope(period)
            val routes = async { runCatching { repository.getAssignedRoutes() }.getOrDefault(_coverage.value.routes) }
            val month = async { metrics(bounds.start, bounds.end, routeId) }
            val primary = async { metrics(scope.primary.start, scope.primary.end, routeId) }
            val comparison = async { metrics(scope.comparison.start, scope.comparison.end, routeId) }
            val routeList = routes.await()
            _coverage.value = CoverageState(
                isLoading = false,
                routes = routeList,
                routeId = routeId?.takeIf { id -> routeList.any { it.id == id } },
                month = month.await(),
                scope = scope,
                primary = primary.await(),
                comparison = comparison.await()
            )
        }
    }

    fun loadConversion() {
        viewModelScope.launch {
            _conversion.update { it.copy(isLoading = true) }
            val period = _period.value
            val bounds = monthBounds(period.month, period.year)
            val scope = resolveKpiComparisonScope(period)
            val kpi = async { runCatching { repository.getAgentKpi(period.month, period.year).toDomain() }.getOrNull() }
            val primary = async { metrics(scope.primary.start, scope.primary.end, null) }
            val comparison = async { metrics(scope.comparison.start, scope.comparison.end, null) }
            val reason = async { runCatching { repository.getTopNoOrderReason(bounds.start, bounds.end) }.getOrNull() }
            _conversion.value = ConversionState(false, kpi.await(), scope, primary.await(), comparison.await(), reason.await())
        }
    }

    fun loadReport() {
        viewModelScope.launch {
            _report.update { it.copy(isLoading = true) }
            val period = _period.value
            val kpi = async { runCatching { repository.getAgentKpi(period.month, period.year).toDomain() }.getOrNull() }
            val coverage = async { runCatching { loadCoverageFor(period) }.getOrNull() }
            _report.value = ReportState(false, kpi.await(), coverage.await())
        }
    }

    private suspend fun metrics(start: String, end: String, routeId: String?): RoutePeriodMetricsRow? =
        runCatching { repository.getRoutePeriodMetrics(start, end, routeId) }
            .onFailure { Timber.w(it, "Route period metrics failed") }
            .getOrNull()
}
