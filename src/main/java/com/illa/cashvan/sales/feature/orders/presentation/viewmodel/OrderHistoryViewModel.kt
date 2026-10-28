package com.illa.cashvan.sales.feature.orders.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.analytics.SalesAnalytics
import com.illa.cashvan.sales.feature.orders.data.model.PresellHistoryOrder
import com.illa.cashvan.sales.feature.orders.domain.repository.CancelOrderException
import com.illa.cashvan.sales.feature.orders.domain.repository.PresellOrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class OrderHistoryUiState(
    val isLoading: Boolean = true,
    val orders: List<PresellHistoryOrder> = emptyList(),
    val search: String = "",
    val failed: Boolean = false,
    val cancellingId: String? = null,
    val message: String? = null
) {
    /** Matches the merchant's name or code, as the PWA's `filterOrdersByMerchant`. */
    val visibleOrders: List<PresellHistoryOrder>
        get() {
            val query = search.trim().lowercase()
            if (query.isEmpty()) return orders
            return orders.filter { order ->
                listOfNotNull(order.merchant?.name, order.merchant?.merchant_code).any { it.lowercase().contains(query) }
            }
        }
}

class OrderHistoryViewModel(
    private val orderRepository: PresellOrderRepository,
    analytics: SalesAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderHistoryUiState())
    val uiState: StateFlow<OrderHistoryUiState> = _uiState.asStateFlow()

    init {
        analytics.track("view_order_history")
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, failed = false) }
            try {
                val orders = orderRepository.getMonthOrders()
                _uiState.update { it.copy(isLoading = false, orders = orders) }
            } catch (e: Exception) {
                Timber.e(e, "Order history failed")
                _uiState.update { it.copy(isLoading = false, failed = true) }
            }
        }
    }

    fun onSearch(query: String) {
        _uiState.update { it.copy(search = query) }
    }

    fun cancel(orderId: String) {
        if (_uiState.value.cancellingId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(cancellingId = orderId) }
            val message = try {
                orderRepository.cancelOrder(orderId)
                load()
                "تم إلغاء الطلب"
            } catch (e: CancelOrderException) {
                when (e.code) {
                    "GRACE_PERIOD_EXPIRED" -> "انتهت مهلة الإلغاء"
                    "ORDER_NOT_UNFULFILLED" -> "لم يعد الطلب متاحاً للإلغاء"
                    else -> "تعذر إلغاء الطلب، حاول مرة أخرى"
                }
            } catch (e: Exception) {
                Timber.e(e, "Cancel order failed")
                "تعذر إلغاء الطلب، حاول مرة أخرى"
            }
            _uiState.update { it.copy(cancellingId = null, message = message) }
        }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }
}
