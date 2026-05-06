package com.illa.cashvan.feature.plans.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.OngoingPlanResponse
import com.illa.cashvan.feature.orders.domain.usecase.GetOngoingPlanUseCase
import com.illa.cashvan.feature.plans.data.model.Plan
import com.illa.cashvan.feature.plans.data.model.PlanProduct
import com.illa.cashvan.feature.plans.domain.usecase.GetPlanProductsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InventoryUiState(
    val isLoading: Boolean = false,
    val planProducts: List<PlanProduct> = emptyList(),
    val plan: OngoingPlanResponse? = null,
    val selectedPlan: Plan? = null,
    val error: String? = null,
    val currentPage: Int = 1,
    val hasMoreProducts: Boolean = true,
    val isLoadingMore: Boolean = false
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
                is ApiResult.Loading -> {}
            }
        }
    }


    fun loadPlanProducts(planId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                currentPage = 1,
                hasMoreProducts = true
            )

            when (val result = getPlanProductsUseCase(planId, page = 1)) {
                is ApiResult.Success -> {
                    val products = result.data.plan_products
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        planProducts = products,
                        currentPage = 1,
                        hasMoreProducts = products.size >= 20,
                        error = null
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun loadMorePlanProducts() {
        val state = _uiState.value
        val planId = state.plan?.id ?: return
        if (!state.hasMoreProducts || state.isLoadingMore || state.isLoading) return

        val nextPage = state.currentPage + 1
        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)

            when (val result = getPlanProductsUseCase(planId, page = nextPage)) {
                is ApiResult.Success -> {
                    val newProducts = result.data.plan_products
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        planProducts = _uiState.value.planProducts + newProducts,
                        currentPage = nextPage,
                        hasMoreProducts = newProducts.size >= 20
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
                is ApiResult.Loading -> {}
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