package com.illa.cashvan.feature.plans.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.OngoingPlanResponse
import com.illa.cashvan.feature.orders.domain.usecase.GetOngoingPlanUseCase
import com.illa.cashvan.feature.plans.data.model.Plan
import com.illa.cashvan.feature.plans.data.model.PlanProduct
import com.illa.cashvan.feature.plans.domain.usecase.GetPlanProductsUseCase
import com.illa.cashvan.feature.plans.domain.usecase.GetPlansUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InventoryUiState(
    val isLoading: Boolean = false,
    val planProducts: List<PlanProduct> = emptyList(),
    val plan: OngoingPlanResponse?=null,
    val selectedPlan: Plan? = null,
    val error: String? = null
)

class InventoryViewModel(
    private val getPlanProductsUseCase: GetPlanProductsUseCase,
    private val getOngoingPlanUseCase: GetOngoingPlanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        loadPlans()
    }

    private fun loadPlans() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            when (val result = getOngoingPlanUseCase()) {
                is ApiResult.Success -> {
                    val plan: OngoingPlanResponse? = result.data
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        plan = plan,
                        error = null
                    )
                    if (plan != null && plan.id != null) {
                        loadPlanProducts(plan.id)
                    }
                    else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            planProducts = emptyList(),
                            error = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                        planProducts = emptyList()
                    )
                }
                is ApiResult.Loading -> {
                    // Loading state is already set above
                }
            }
        }
    }


    fun loadPlanProducts(planId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            when (val result = getPlanProductsUseCase(planId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        planProducts = result.data.plan_products,
                        error = null
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is ApiResult.Loading -> {
                }
            }
        }
    }

    fun refresh() {
        val currentPlan = _uiState.value.selectedPlan
        if (currentPlan != null) {
            loadPlanProducts(currentPlan.id)
        } else {
            loadPlans()
        }
    }
}