package com.illa.cashvan.feature.plans.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.network.model.ApiResult
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
    val plans: List<Plan> = emptyList(),
    val selectedPlan: Plan? = null,
    val error: String? = null
)

class InventoryViewModel(
    private val getPlanProductsUseCase: GetPlanProductsUseCase,
    private val getPlansUseCase: GetPlansUseCase
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

            when (val result = getPlansUseCase()) {
                is ApiResult.Success -> {
                    val plans = result.data.plans
                    val selectedPlan = plans.firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        plans = plans,
                        selectedPlan = selectedPlan,
                        error = null
                    )

                    // Auto-load products for the first plan
                    selectedPlan?.let { plan ->
                        loadPlanProducts(plan.id.toString())
                    }
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is ApiResult.Loading -> {
                    // Loading state is already set above
                }
            }
        }
    }

    fun selectPlan(plan: Plan) {
        _uiState.value = _uiState.value.copy(selectedPlan = plan)
        loadPlanProducts(plan.id.toString())
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
            loadPlanProducts(currentPlan.id.toString())
        } else {
            loadPlans()
        }
    }
}