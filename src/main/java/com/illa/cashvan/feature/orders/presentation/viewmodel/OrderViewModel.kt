package com.illa.cashvan.feature.orders.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.getCurrentDateApiFormat
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.Order
import com.illa.cashvan.feature.orders.data.model.UpdateOrderData
import com.illa.cashvan.feature.orders.data.model.UpdateOrderRequest
import com.illa.cashvan.feature.orders.domain.usecase.GetOrdersUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetOrderByIdUseCase
import com.illa.cashvan.feature.orders.domain.usecase.UpdateOrderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OrderType(val value: String, val displayName: String) {
    CASH_VAN("cash_van", "أوردارات كاش"),
    PRE_SELL("pre_sell", "أوردرات تسليم")
}

data class OrderUiState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val error: String? = null,
    val selectedTab: OrderType = OrderType.PRE_SELL
)

data class OrderDetailsUiState(
    val isLoading: Boolean = false,
    val order: Order? = null,
    val error: String? = null
)

class OrderViewModel(
    private val getOrdersUseCase: GetOrdersUseCase,
    private val getOrderByIdUseCase: GetOrderByIdUseCase,
    private val updateOrderUseCase: UpdateOrderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    private val _orderDetailsUiState = MutableStateFlow(OrderDetailsUiState())
    val orderDetailsUiState: StateFlow<OrderDetailsUiState> = _orderDetailsUiState.asStateFlow()

    init {
        loadOrders(orderType = OrderType.PRE_SELL)
    }

    fun loadOrders(date: String? = null, orderType: OrderType = _uiState.value.selectedTab) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Always use today's date
            val dateToUse = getCurrentDateApiFormat()

            when (val result = getOrdersUseCase(dateToUse, orderType.value)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        orders = result.data.data
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun selectTab(orderType: OrderType) {
        if (_uiState.value.selectedTab != orderType) {
            _uiState.value = _uiState.value.copy(selectedTab = orderType)
            loadOrders(orderType = orderType)
        }
    }

    fun refresh() {
        loadOrders()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun loadOrderById(orderId: String) {
        viewModelScope.launch {
            _orderDetailsUiState.value = _orderDetailsUiState.value.copy(isLoading = true, error = null)

            when (val result = getOrderByIdUseCase(orderId)) {
                is ApiResult.Success -> {
                    _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                        isLoading = false,
                        order = result.data
                    )
                }
                is ApiResult.Error -> {
                    _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is ApiResult.Loading -> {
                    _orderDetailsUiState.value = _orderDetailsUiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun clearOrderDetailsError() {
        _orderDetailsUiState.value = _orderDetailsUiState.value.copy(error = null)
    }

    fun cancelOrder(orderId: String, reason: String, note: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val request = UpdateOrderRequest(
                order = UpdateOrderData(
                    status = "canceled",
                    cancellation_reason = reason,
                    cancellation_note = note
                )
            )

            when (val result = updateOrderUseCase(orderId, request)) {
                is ApiResult.Success -> {
                    onSuccess()
                    // Refresh the orders list
                    loadOrders()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is ApiResult.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    fun submitOrder(order: Order, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            // Map order plan products to submit order items
            val orderItems = order.order_plan_products?.map { planProduct ->
                com.illa.cashvan.feature.orders.data.model.SubmitOrderItem(
                    plan_product_id = planProduct.plan_product_id ?: "",
                    sold_quantity = planProduct.sold_quantity
                )
            } ?: emptyList()

            val request = UpdateOrderRequest(
                order = UpdateOrderData(
                    status = "submitted",
                    order_items = orderItems
                )
            )

            when (val result = updateOrderUseCase(order.id, request)) {
                is ApiResult.Success -> {
                    onSuccess()
                    // Refresh the orders list
                    loadOrders()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is ApiResult.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }
}