package com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanOrderRow
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanPlan
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanPlanProductRow
import com.illa.cashvan.sales.feature.cashvan.domain.repository.CashVanRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class CashVanHomeUiState(
    val isLoading: Boolean = true,
    val failed: Boolean = false,
    val plan: CashVanPlan? = null,
    val products: List<CashVanPlanProductRow> = emptyList(),
    val orders: List<CashVanOrderRow> = emptyList()
) {
    val noPlan: Boolean get() = !isLoading && !failed && plan == null
    val availableProductCount: Int get() = products.count { it.cash_van_available_quantity > 0 }
    val assignedTotal: Int get() = products.sumOf { it.assigned_quantity }
    val soldTotal: Int get() = products.sumOf { it.sold_quantity }
    val availableTotal: Int get() = products.sumOf { maxOf(it.cash_van_available_quantity, 0) }
}

/** The PWA's `CashvanDashboard`: the truck's stock and the plan's recent cash van orders. */
class CashVanHomeViewModel(
    private val repository: CashVanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CashVanHomeUiState())
    val uiState: StateFlow<CashVanHomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, failed = false) }
            try {
                val plan = repository.getOngoingPlan()
                val planId = plan?.id
                if (planId == null) {
                    _uiState.value = CashVanHomeUiState(isLoading = false)
                    return@launch
                }
                val products = async { repository.getPlanProducts(planId, merchantId = null) }
                val orders = async { repository.getPlanOrders(planId) }
                _uiState.value = CashVanHomeUiState(
                    isLoading = false,
                    plan = plan,
                    products = products.await(),
                    orders = orders.await()
                )
            } catch (e: Exception) {
                Timber.e(e, "Cash van dashboard failed")
                _uiState.update { it.copy(isLoading = false, failed = true) }
            }
        }
    }
}
