package com.illa.cashvan.feature.merchant.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.merchant.data.model.CreateMerchantResponse
import com.illa.cashvan.feature.merchant.domain.usecase.CreateMerchantUseCase
import com.illa.cashvan.feature.plans.data.model.Plan
import com.illa.cashvan.feature.plans.domain.usecase.GetPlansUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateMerchantUiState(
    val isLoading: Boolean = false,
    val merchant: CreateMerchantResponse? = null,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val plans: List<Plan> = emptyList(),
    val isPlansLoading: Boolean = false,
    val plansError: String? = null
)

class MerchantViewModel(
    private val createMerchantUseCase: CreateMerchantUseCase,
    private val getPlansUseCase: GetPlansUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateMerchantUiState())
    val uiState: StateFlow<CreateMerchantUiState> = _uiState.asStateFlow()

    init {
        getPlans()
    }

    fun createMerchant(
        name: String,
        phoneNumber: String,
        latitude: String,
        longitude: String,
        planId: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                isSuccess = false
            )

            // Add "2" prefix to phone number
            val formattedPhoneNumber = "2$phoneNumber"

            when (val result = createMerchantUseCase.invoke(name, formattedPhoneNumber, latitude, longitude, planId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        merchant = result.data,
                        error = null,
                        isSuccess = true
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                        isSuccess = false
                    )
                }
                is ApiResult.Loading -> {
                }
            }
        }
    }

    fun getPlans() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPlansLoading = true, plansError = null)

            when (val result = getPlansUseCase()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isPlansLoading = false,
                        plans = result.data.plans,
                        plansError = null
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isPlansLoading = false,
                        plansError = result.message
                    )
                }
                is ApiResult.Loading -> {
                }
            }
        }
    }

    fun getFirstPlanId(): String? {
        return _uiState.value.plans.firstOrNull()?.id?.toString()
    }


    fun resetState() {
        _uiState.value = CreateMerchantUiState()
    }
}