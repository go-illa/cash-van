package com.illa.cashvan.sales.feature.route.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.analytics.SalesAnalytics
import com.illa.cashvan.sales.domain.model.PresellOrderContext
import com.illa.cashvan.sales.feature.route.data.model.AdHocMerchant
import com.illa.cashvan.sales.feature.route.data.model.RouteRef
import com.illa.cashvan.sales.feature.route.domain.repository.RouteRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class AdHocUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<AdHocMerchant> = emptyList(),
    val routesByMerchant: Map<String, List<RouteRef>> = emptyMap(),
    /** Set when the merchant is on several of the agent's routes and one must be picked. */
    val pickingRouteFor: AdHocMerchant? = null,
    val error: String? = null
)

/**
 * The PWA's ad-hoc order button: search the merchants on the agent's routes and order for one outside
 * today's plan. Ad-hoc visits don't count towards the route's monthly progress.
 */
@OptIn(FlowPreview::class)
class AdHocOrderViewModel(
    private val routeRepository: RouteRepository,
    private val analytics: SalesAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdHocUiState())
    val uiState: StateFlow<AdHocUiState> = _uiState.asStateFlow()

    private val queries = MutableStateFlow("")

    init {
        analytics.track("ad_hoc_search_opened")
        viewModelScope.launch {
            val routes = runCatching { routeRepository.getRouteOptionsByMerchant() }
                .onFailure { Timber.e(it, "Ad-hoc route options failed") }
                .getOrDefault(emptyMap())
            _uiState.update { it.copy(routesByMerchant = routes) }
            queries.debounce(SEARCH_DEBOUNCE_MS).collectLatest(::search)
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        queries.value = query
    }

    private suspend fun search(query: String) {
        val ids = _uiState.value.routesByMerchant.keys
        if (query.isBlank() || ids.isEmpty()) {
            _uiState.update { it.copy(results = emptyList(), isLoading = false) }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val results = routeRepository.searchMerchants(ids, query)
            _uiState.update { it.copy(results = results, isLoading = false) }
            analytics.track("ad_hoc_search_query_typed", "query_length" to query.trim().length, "results_count" to results.size)
        } catch (e: Exception) {
            Timber.e(e, "Ad-hoc search failed")
            _uiState.update { it.copy(results = emptyList(), isLoading = false, error = "فشل البحث عن التجار") }
        }
    }

    /** Returns the order context when the route is known; otherwise asks for one first. */
    fun select(merchant: AdHocMerchant): PresellOrderContext? {
        val routes = _uiState.value.routesByMerchant[merchant.id].orEmpty()
        if (routes.size > 1) {
            _uiState.update { it.copy(pickingRouteFor = merchant) }
            return null
        }
        return contextFor(merchant, routes.firstOrNull())
    }

    fun selectRoute(route: RouteRef): PresellOrderContext? {
        val merchant = _uiState.value.pickingRouteFor ?: return null
        _uiState.update { it.copy(pickingRouteFor = null) }
        return contextFor(merchant, route)
    }

    fun cancelRoutePick() {
        _uiState.update { it.copy(pickingRouteFor = null) }
    }

    private fun contextFor(merchant: AdHocMerchant, route: RouteRef?): PresellOrderContext {
        analytics.track(
            "merchant_selected",
            "merchant_id" to merchant.id,
            "merchant_name" to merchant.displayName,
            "merchant_code" to merchant.merchant_code,
            "price_tier" to (merchant.price_tier ?: "retail")
        )
        return buildContext(merchant, route)
    }

    private fun buildContext(merchant: AdHocMerchant, route: RouteRef?) = PresellOrderContext(
        merchantId = merchant.id,
        merchantName = merchant.displayName,
        priceTier = merchant.price_tier ?: "retail",
        regionId = merchant.region_id,
        routeId = route?.id,
        routeName = route?.name,
        isAdHoc = true,
        visitLocation = null
    )

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
