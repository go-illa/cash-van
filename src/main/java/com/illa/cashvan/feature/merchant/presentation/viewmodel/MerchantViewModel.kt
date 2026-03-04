package com.illa.cashvan.feature.merchant.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.merchant.data.model.CreateMerchantResponse
import com.illa.cashvan.feature.merchant.data.model.Governorate
import com.illa.cashvan.feature.merchant.data.model.MerchantType
import com.illa.cashvan.feature.merchant.domain.usecase.CreateMerchantUseCase
import com.illa.cashvan.feature.merchant.domain.usecase.GetGovernoratesUseCase
import com.illa.cashvan.feature.merchant.domain.usecase.GetMerchantTypesUseCase
import com.illa.cashvan.feature.merchant.domain.usecase.GetReverseGeocodeUseCase
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
    val plansError: String? = null,
    val governorates: List<Governorate> = emptyList(),
    val isGovernoratesLoading: Boolean = false,
    val governoratesError: String? = null,
    val merchantTypes: List<MerchantType> = emptyList(),
    val isMerchantTypesLoading: Boolean = false,
    val merchantTypesError: String? = null,
    val reverseGeocodeAddress: String? = null,
    val reverseGeocodeGovernorateName: String? = null,
    val isReverseGeocodeLoading: Boolean = false
)

class MerchantViewModel(
    private val createMerchantUseCase: CreateMerchantUseCase,
    private val getPlansUseCase: GetPlansUseCase,
    private val getGovernoratesUseCase: GetGovernoratesUseCase,
    private val getMerchantTypesUseCase: GetMerchantTypesUseCase,
    private val getReverseGeocodeUseCase: GetReverseGeocodeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateMerchantUiState())
    val uiState: StateFlow<CreateMerchantUiState> = _uiState.asStateFlow()

    init {
        getPlans()
        getGovernorates()
        getMerchantTypes()
    }

    fun createMerchant(
        name: String,
        signName: String,
        phoneNumber: String,
        secondaryPhoneNumber: String?,
        latitude: Double,
        longitude: Double,
        planId: Int,
        merchantTypeId: String,
        detailedAddress: String,
        priceTier: String,
        visitDays: Set<String>
    ) {
        val allDays = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
        val workingDays = allDays.associateWith { it in visitDays }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, isSuccess = false)

            val formattedPhone = "2$phoneNumber"
            val formattedSecondaryPhone = secondaryPhoneNumber?.takeIf { it.isNotBlank() }?.let { "2$it" }

            when (val result = createMerchantUseCase.invoke(
                name = name,
                signName = signName,
                phoneNumber = formattedPhone,
                secondaryPhoneNumber = formattedSecondaryPhone,
                latitude = latitude,
                longitude = longitude,
                planId = planId,
                merchantTypeId = merchantTypeId,
                detailedAddress = detailedAddress,
                priceTier = priceTier,
                workingDays = workingDays
            )) {
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
                is ApiResult.Loading -> {}
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
                    _uiState.value = _uiState.value.copy(isPlansLoading = false, plansError = result.message)
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun getGovernorates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGovernoratesLoading = true, governoratesError = null)

            when (val result = getGovernoratesUseCase.invoke()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isGovernoratesLoading = false,
                        governorates = result.data.governorates,
                        governoratesError = null
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isGovernoratesLoading = false,
                        governoratesError = result.message
                    )
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun getMerchantTypes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMerchantTypesLoading = true, merchantTypesError = null)

            when (val result = getMerchantTypesUseCase.invoke()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isMerchantTypesLoading = false,
                        merchantTypes = result.data.merchant_types,
                        merchantTypesError = null
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isMerchantTypesLoading = false,
                        merchantTypesError = result.message
                    )
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun loadReverseGeocode(latitude: String, longitude: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isReverseGeocodeLoading = true)

            when (val result = getReverseGeocodeUseCase.invoke(latitude, longitude)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isReverseGeocodeLoading = false,
                        reverseGeocodeAddress = result.data.detailed_address,
                        reverseGeocodeGovernorateName = result.data.components.governorate
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isReverseGeocodeLoading = false)
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun getFirstPlanId(): Int? {
        return _uiState.value.plans.firstOrNull()?.id?.toIntOrNull()
    }

    fun resetState() {
        _uiState.value = CreateMerchantUiState()
    }
}
