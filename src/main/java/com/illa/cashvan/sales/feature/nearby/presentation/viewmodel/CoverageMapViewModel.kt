package com.illa.cashvan.sales.feature.nearby.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.location.LocationData
import com.illa.cashvan.core.location.LocationService
import com.illa.cashvan.sales.domain.model.RouteMerchant
import com.illa.cashvan.sales.feature.route.domain.repository.RouteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** Pin colour groups: not visited, visited once, visited twice (the monthly target). */
enum class CoverageBucket { UNVISITED, ONCE, COMPLETED }

fun RouteMerchant.coverageBucket(): CoverageBucket = when {
    visitCount >= 2 -> CoverageBucket.COMPLETED
    visitCount == 1 -> CoverageBucket.ONCE
    else -> CoverageBucket.UNVISITED
}

data class CoverageMapUiState(
    val isLoading: Boolean = true,
    val merchants: List<RouteMerchant> = emptyList(),
    val agentLocation: LocationData? = null,
    val search: String = "",
    /** Empty means every bucket shows (the legend doubles as a filter). */
    val buckets: Set<CoverageBucket> = emptySet(),
    val selectedId: String? = null
) {
    val visible: List<RouteMerchant>
        get() {
            val term = search.trim().lowercase()
            return merchants.filter { merchant ->
                (buckets.isEmpty() || merchant.coverageBucket() in buckets) &&
                    (term.isEmpty() || listOfNotNull(merchant.merchantName, merchant.signName, merchant.merchantCode, merchant.phoneNumber)
                        .any { it.lowercase().contains(term) })
            }
        }

    fun count(bucket: CoverageBucket): Int = merchants.count { it.coverageBucket() == bucket }
    val selected: RouteMerchant? get() = merchants.firstOrNull { it.merchantId == selectedId }
}

/** "خريطة التغطية": the agent's route merchants on a map, coloured by this month's visits. */
class CoverageMapViewModel(
    private val routeRepository: RouteRepository,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoverageMapUiState())
    val uiState: StateFlow<CoverageMapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val location = locationService.getCurrentLocation().getOrNull()
            _uiState.update { it.copy(agentLocation = location) }
        }
        viewModelScope.launch {
            val merchants = runCatching { routeRepository.getRouteMerchants() }
                .onFailure { Timber.e(it, "Coverage map failed") }
                .getOrDefault(emptyList())
                .filter { it.hasCoordinates }
            _uiState.update { it.copy(isLoading = false, merchants = merchants) }
        }
    }

    fun onSearch(query: String) {
        _uiState.update { it.copy(search = query) }
    }

    fun toggleBucket(bucket: CoverageBucket) {
        _uiState.update { state ->
            state.copy(buckets = if (bucket in state.buckets) state.buckets - bucket else state.buckets + bucket)
        }
    }

    fun clearBuckets() {
        _uiState.update { it.copy(buckets = emptySet()) }
    }

    fun select(merchantId: String?) {
        _uiState.update { it.copy(selectedId = merchantId) }
    }
}
