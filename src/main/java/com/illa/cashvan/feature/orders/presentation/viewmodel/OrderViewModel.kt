package com.illa.cashvan.feature.orders.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.Order
import com.illa.cashvan.feature.orders.domain.usecase.GetOrdersUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetOrderByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrderUiState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val error: String? = null
)

data class OrderDetailsUiState(
    val isLoading: Boolean = false,
    val order: Order? = null,
    val error: String? = null
)

class OrderViewModel(
    private val getOrdersUseCase: GetOrdersUseCase,
    private val getOrderByIdUseCase: GetOrderByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    private val _orderDetailsUiState = MutableStateFlow(OrderDetailsUiState())
    val orderDetailsUiState: StateFlow<OrderDetailsUiState> = _orderDetailsUiState.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders(date: String = "2025-09-28") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = getOrdersUseCase(date)) {
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
}