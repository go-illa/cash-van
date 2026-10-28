package com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.sales.domain.cashvan.CashvanPlanType
import com.illa.cashvan.sales.domain.cashvan.RailsOrder
import com.illa.cashvan.sales.domain.reconciliation.DailyReconciliationTotals
import com.illa.cashvan.sales.domain.reconciliation.PrincipalGroup
import com.illa.cashvan.sales.domain.reconciliation.computeMoneyBreakdown
import com.illa.cashvan.sales.domain.reconciliation.computePlanProductBreakdown
import com.illa.cashvan.sales.domain.reconciliation.filterReconciledOrders
import com.illa.cashvan.sales.domain.reconciliation.groupRowsByPrincipal
import com.illa.cashvan.sales.feature.cashvan.data.mapper.toRailsOrder
import com.illa.cashvan.sales.feature.cashvan.data.mapper.toRailsPlanProduct
import com.illa.cashvan.sales.feature.cashvan.domain.repository.CashVanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class ReconciliationReport(
    val planCode: String,
    val planType: CashvanPlanType?,
    val totals: DailyReconciliationTotals,
    val groups: List<PrincipalGroup>,
    /** The orders counted in [totals], newest first, for the PDF's order table. */
    val orders: List<RailsOrder>
)

data class ReconciliationUiState(
    val isLoading: Boolean = true,
    val noPlan: Boolean = false,
    val failed: Boolean = false,
    val report: ReconciliationReport? = null
)

/** The PWA's daily reconciliation: money by payment type and stock by product over the whole open plan. */
class ReconciliationViewModel(
    private val repository: CashVanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReconciliationUiState())
    val uiState: StateFlow<ReconciliationUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, failed = false) }
            _uiState.value = try {
                buildReport()?.let { ReconciliationUiState(isLoading = false, report = it) }
                    ?: ReconciliationUiState(isLoading = false, noPlan = true)
            } catch (e: Exception) {
                Timber.e(e, "Reconciliation failed")
                ReconciliationUiState(isLoading = false, failed = true)
            }
        }
    }

    /** Re-reads the plan so the PDF is as fresh as possible, as the PWA does before exporting. */
    suspend fun freshReport(): ReconciliationReport? = runCatching { buildReport() }.getOrNull()

    private suspend fun buildReport(): ReconciliationReport? {
        val planId = repository.getOngoingPlan()?.id ?: return null
        val detail = repository.getPlanDetail(planId) ?: return null
        val planType = CashvanPlanType.fromWire(detail.plan_type)
        val orders = detail.orders.map { it.toRailsOrder() }
        val rows = computePlanProductBreakdown(detail.plan_products.map { it.toRailsPlanProduct() }, planType)
        val principals = repository.getPrincipalNamesBySku(rows.map { it.sku }.filter { it.isNotEmpty() })
        return ReconciliationReport(
            planCode = detail.code.orEmpty(),
            planType = planType,
            totals = computeMoneyBreakdown(orders),
            groups = groupRowsByPrincipal(rows, principals),
            orders = filterReconciledOrders(orders).sortedByDescending { it.createdAt }
        )
    }
}
