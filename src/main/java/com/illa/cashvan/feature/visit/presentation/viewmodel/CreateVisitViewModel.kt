package com.illa.cashvan.feature.visit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.location.LocationService
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.MerchantItem
import com.illa.cashvan.feature.orders.domain.usecase.SearchMerchantsUseCase
import com.illa.cashvan.feature.visit.data.model.CreateVisitRequest
import com.illa.cashvan.feature.visit.data.model.NoOrderReason
import com.illa.cashvan.feature.visit.data.model.VisitData
import com.illa.cashvan.feature.visit.domain.usecase.CreateVisitUseCase
import com.illa.cashvan.feature.visit.domain.usecase.GetNoOrderReasonsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class CreateVisitUiState(
    val isLoading: Boolean = false,
    val merchants: List<MerchantItem> = emptyList(),
    val merchantPage: Int = 1,
    val hasMoreMerchants: Boolean = true,
    val isLoadingMoreMerchants: Boolean = false,
    val merchantSearchQuery: String = "",
    val isSearchingMerchants: Boolean = false,
    val selectedMerchant: MerchantItem? = null,
    val noOrderReasons: List<NoOrderReason> = emptyList(),
    val isLoadingReasons: Boolean = false,
    val selectedReason: NoOrderReason? = null,
    val visitCreated: Boolean = false,
    val visitCreationError: String? = null,
    val locationGranted: Boolean = false,
    val isGettingLocation: Boolean = false,
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    val error: String? = null
)

class CreateVisitViewModel(
    private val locationService: LocationService,
    private val searchMerchantsUseCase: SearchMerchantsUseCase,
    private val getNoOrderReasonsUseCase: GetNoOrderReasonsUseCase,
    private val createVisitUseCase: CreateVisitUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateVisitUiState())
    val uiState: StateFlow<CreateVisitUiState> = _uiState.asStateFlow()

    private var merchantSearchJob: Job? = null

    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGettingLocation = true)
            locationService.getCurrentLocation()
                .onSuccess { location ->
                    _uiState.value = _uiState.value.copy(
                        isGettingLocation = false,
                        locationGranted = true,
                        userLatitude = location.latitude,
                        userLongitude = location.longitude
                    )
                    searchMerchants("")
                }
                .onFailure {
                    locationService.getLastKnownLocation()
                        .onSuccess { location ->
                            _uiState.value = _uiState.value.copy(
                                isGettingLocation = false,
                                locationGranted = true,
                                userLatitude = location.latitude,
                                userLongitude = location.longitude
                            )
                            searchMerchants("")
                        }
                        .onFailure {
                            _uiState.value = _uiState.value.copy(
                                isGettingLocation = false,
                                error = "تعذر تحديد الموقع، يرجى المحاولة مجدداً"
                            )
                        }
                }
        }
    }

    fun onLocationPermissionDenied() {
        _uiState.value = _uiState.value.copy(locationGranted = false, isGettingLocation = false)
    }

    fun searchMerchants(query: String) {
        val lat = _uiState.value.userLatitude ?: return
        val lon = _uiState.value.userLongitude ?: return
        _uiState.value = _uiState.value.copy(merchantSearchQuery = query, merchantPage = 1, hasMoreMerchants = true)
        merchantSearchJob?.cancel()
        merchantSearchJob = viewModelScope.launch {
            delay(300)
            _uiState.value = _uiState.value.copy(isSearchingMerchants = true)
            try {
                when (val result = searchMerchantsUseCase(query, lat, lon, page = 1)) {
                    is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                        isSearchingMerchants = false,
                        merchants = result.data.merchants,
                        hasMoreMerchants = result.data.merchants.size >= 20
                    )
                    is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                        isSearchingMerchants = false,
                        error = result.message
                    )
                    is ApiResult.Loading -> {}
                }
            } finally {
                _uiState.value = _uiState.value.copy(isSearchingMerchants = false)
            }
        }
    }

    fun loadMoreMerchants() {
        val state = _uiState.value
        if (!state.hasMoreMerchants || state.isLoadingMoreMerchants || state.isSearchingMerchants) return
        val lat = state.userLatitude ?: return
        val lon = state.userLongitude ?: return
        val nextPage = state.merchantPage + 1
        _uiState.value = state.copy(isLoadingMoreMerchants = true)
        viewModelScope.launch {
            try {
                when (val result = searchMerchantsUseCase(state.merchantSearchQuery, lat, lon, page = nextPage)) {
                    is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                        isLoadingMoreMerchants = false,
                        merchants = _uiState.value.merchants + result.data.merchants,
                        merchantPage = nextPage,
                        hasMoreMerchants = result.data.merchants.size >= 20
                    )
                    is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoadingMoreMerchants = false)
                    is ApiResult.Loading -> {}
                }
            } finally {
                _uiState.value = _uiState.value.copy(isLoadingMoreMerchants = false)
            }
        }
    }

    fun selectMerchant(merchant: MerchantItem) {
        _uiState.value = _uiState.value.copy(
            selectedMerchant = merchant,
            merchantSearchQuery = merchant.displayName,
            selectedReason = null,
            noOrderReasons = emptyList()
        )
        loadNoOrderReasons()
    }

    fun clearMerchant() {
        _uiState.value = _uiState.value.copy(
            selectedMerchant = null,
            merchantSearchQuery = "",
            selectedReason = null,
            noOrderReasons = emptyList()
        )
    }

    private fun loadNoOrderReasons() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingReasons = true)
            when (val result = getNoOrderReasonsUseCase()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoadingReasons = false,
                    noOrderReasons = result.data.no_order_reasons.filter { it.is_active }
                )
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoadingReasons = false,
                    error = result.message
                )
                is ApiResult.Loading -> {}
            }
        }
    }

    fun selectReason(reason: NoOrderReason) {
        _uiState.value = _uiState.value.copy(selectedReason = reason)
    }

    fun createVisit() {
        val state = _uiState.value
        val merchant = state.selectedMerchant ?: return
        val reason = state.selectedReason ?: return
        val lat = state.userLatitude ?: return
        val lon = state.userLongitude ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .format(Date())

            val request = CreateVisitRequest(
                visit = VisitData(
                    merchant_id = merchant.id,
                    visit_type = "without_order",
                    visit_date = isoDate,
                    latitude = lat,
                    longitude = lon,
                    visit_quality = 1,
                    no_order_reason_id = reason.id
                )
            )
            when (val result = createVisitUseCase(request)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, visitCreated = true)
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, visitCreationError = result.message)
                is ApiResult.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearVisitCreationError() {
        _uiState.value = _uiState.value.copy(visitCreationError = null)
    }

    fun resetVisitCreated() {
        _uiState.value = _uiState.value.copy(visitCreated = false)
    }
}
