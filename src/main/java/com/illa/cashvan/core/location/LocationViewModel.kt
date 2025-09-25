package com.illa.cashvan.core.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LocationUiState(
    val isLoading: Boolean = false,
    val locationData: LocationData? = null,
    val error: String? = null,
    val hasPermission: Boolean = false
)

class LocationViewModel(
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    fun onPermissionGranted() {
        _uiState.value = _uiState.value.copy(hasPermission = true)
        getCurrentLocation()
    }

    fun onPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            hasPermission = false,
            error = "Location permission is required"
        )
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = locationService.getCurrentLocation()
            result.fold(
                onSuccess = { location ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        locationData = location,
                        error = null
                    )
                },
                onFailure = { exception ->
                    // Try last known location as fallback
                    val lastKnownResult = locationService.getLastKnownLocation()
                    lastKnownResult.fold(
                        onSuccess = { location ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                locationData = location,
                                error = null
                            )
                        },
                        onFailure = {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Unable to get location"
                            )
                        }
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}