package com.illa.cashvan.sales.feature.route.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.analytics.SalesAnalytics
import com.illa.cashvan.core.location.LocationData
import com.illa.cashvan.core.location.LocationService
import com.illa.cashvan.sales.data.CurrentAgentProvider
import com.illa.cashvan.sales.domain.model.RouteMerchant
import com.illa.cashvan.sales.domain.route.MerchantFilterStatus
import com.illa.cashvan.sales.domain.route.RecentRemoteOrder
import com.illa.cashvan.sales.domain.route.calculateDistance
import com.illa.cashvan.sales.domain.route.matchesMerchantFilter
import com.illa.cashvan.sales.feature.route.domain.repository.RouteRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

enum class LocationStatus { DETECTING, READY, FAILED }

data class RouteListItem(
    val merchant: RouteMerchant,
    /** Meters from the agent, when both locations are known. */
    val distanceMeters: Double?
)

data class RouteGroup(
    /** Null for the single distance-sorted group. */
    val routeName: String?,
    val items: List<RouteListItem>
)

data class RouteUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val agentCode: String? = null,
    val merchants: List<RouteMerchant> = emptyList(),
    val remoteOrders: Map<String, RecentRemoteOrder> = emptyMap(),
    val searchQuery: String = "",
    val filter: MerchantFilterStatus = MerchantFilterStatus.ALL,
    val agentLocation: LocationData? = null,
    val locationStatus: LocationStatus = LocationStatus.DETECTING,
    val groups: List<RouteGroup> = emptyList(),
    val filteredCount: Int = 0
) {
    val routeNames: List<String> get() = merchants.map { it.routeName }.filter { it.isNotEmpty() }.distinct()
    val isFiltering: Boolean get() = searchQuery.isNotBlank() || filter != MerchantFilterStatus.ALL
}

/**
 * The presell home: the agent's monthly journey plan, as the PWA's `Dashboard` shows it.
 */
@OptIn(FlowPreview::class)
class RouteViewModel(
    private val routeRepository: RouteRepository,
    private val locationService: LocationService,
    private val currentAgentProvider: CurrentAgentProvider,
    private val analytics: SalesAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteUiState())
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    private val searchInput = MutableStateFlow("")
    private var searchIndex: Map<String, String> = emptyMap()

    /** Only moves when the agent moves more than [RESORT_DISTANCE_METERS], so GPS jitter doesn't reshuffle the list. */
    private var sortLocation: LocationData? = null
    private var locationJob: Job? = null

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(agentCode = runCatching { currentAgentProvider.require().agentCode }.getOrNull()) }
        }
        viewModelScope.launch {
            searchInput.debounce(SEARCH_DEBOUNCE_MS).collect { query ->
                _uiState.update { it.copy(searchQuery = query) }
                regroup()
                if (query.isNotBlank()) {
                    analytics.track("merchant_search_performed", "query_length" to query.trim().length, "results_count" to _uiState.value.filteredCount)
                }
            }
        }
        load()
    }

    fun load(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = !isRefresh && it.merchants.isEmpty(), isRefreshing = isRefresh, error = null) }
            try {
                val merchants = routeRepository.getRouteMerchants()
                searchIndex = merchants.associate { merchant ->
                    merchant.merchantId to listOf(merchant.merchantName, merchant.signName, merchant.merchantCode, merchant.routeName)
                        .filterNot { it.isNullOrEmpty() }
                        .joinToString(" ")
                        .lowercase()
                }
                _uiState.update { it.copy(merchants = merchants, isLoading = false, isRefreshing = false) }
                regroup()
                loadRemoteOrders(merchants)
            } catch (e: Exception) {
                Timber.e(e, "Route load failed")
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = "تعذر تحميل خط السير. حاول تاني.") }
            }
        }
    }

    fun refresh() {
        analytics.track("dashboard_pull_to_refresh_triggered")
        load(isRefresh = true)
    }

    /** Badges are best-effort: on failure the cards just show none, and the previous badges stay. */
    private suspend fun loadRemoteOrders(merchants: List<RouteMerchant>) {
        runCatching { routeRepository.getRecentRemoteOrders(merchants.map { it.merchantId }) }
            .onSuccess { orders -> _uiState.update { it.copy(remoteOrders = orders) } }
            .onFailure { Timber.w(it, "get_recent_remote_orders failed") }
    }

    fun onSearchChange(query: String) {
        searchInput.value = query
    }

    fun clearFilters() {
        searchInput.value = ""
        _uiState.update { it.copy(searchQuery = "", filter = MerchantFilterStatus.ALL) }
        regroup()
    }

    fun onFilterChange(filter: MerchantFilterStatus) {
        analytics.track("merchant_filter_changed", "filter_status" to filter.name.lowercase())
        _uiState.update { it.copy(filter = filter) }
        regroup()
    }

    fun startLocationUpdates() {
        if (locationJob?.isActive == true) return
        _uiState.update { it.copy(locationStatus = if (it.agentLocation == null) LocationStatus.DETECTING else it.locationStatus) }
        locationJob = viewModelScope.launch {
            locationService.locationUpdates()
                .catch { error ->
                    Timber.w(error, "Location updates failed")
                    _uiState.update { it.copy(locationStatus = LocationStatus.FAILED) }
                }
                .collect { location ->
                    _uiState.update { it.copy(agentLocation = location, locationStatus = LocationStatus.READY) }
                    val previous = sortLocation
                    if (previous == null ||
                        calculateDistance(previous.latitude, previous.longitude, location.latitude, location.longitude) >= RESORT_DISTANCE_METERS
                    ) {
                        sortLocation = location
                    }
                    regroup()
                }
        }
    }

    fun onLocationUnavailable() {
        _uiState.update { it.copy(locationStatus = LocationStatus.FAILED) }
    }

    fun stopLocationUpdates() {
        locationJob?.cancel()
        locationJob = null
    }

    private fun regroup() {
        _uiState.update { state ->
            val query = state.searchQuery.trim().lowercase()
            val filtered = state.merchants.filter { merchant ->
                matchesMerchantFilter(merchant, state.filter) &&
                    (query.isEmpty() || searchIndex[merchant.merchantId].orEmpty().contains(query))
            }
            val agent = state.agentLocation
            fun distanceTo(merchant: RouteMerchant, from: LocationData?): Double? {
                if (from == null || merchant.latitude == null || merchant.longitude == null) return null
                return calculateDistance(from.latitude, from.longitude, merchant.latitude, merchant.longitude)
            }

            val sortFrom = sortLocation
            val groups = if (sortFrom != null) {
                val sorted = filtered.sortedBy { distanceTo(it, sortFrom) ?: Double.POSITIVE_INFINITY }
                listOf(RouteGroup(null, sorted.map { RouteListItem(it, distanceTo(it, agent)) }))
            } else {
                filtered.groupBy { it.routeName }
                    .map { (routeName, merchants) ->
                        RouteGroup(routeName.ifEmpty { UNASSIGNED }, merchants.map { RouteListItem(it, distanceTo(it, agent)) })
                    }
            }
            state.copy(groups = groups, filteredCount = filtered.size)
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 200L
        // The PWA meant 50 m but compares meters to 0.05, so it re-sorts on any jitter.
        const val RESORT_DISTANCE_METERS = 50.0
        const val UNASSIGNED = "غير مخصص"
    }
}
