package com.illa.cashvan.feature.orders.presentation.viewmodel

import com.illa.cashvan.feature.orders.data.model.Order
import com.illa.cashvan.feature.orders.presentation.mapper.ProductPriceInfo

enum class OrderType(val value: String, val displayName: String) {
    CASH_VAN("cash_van", "أوردارات كاش"),
    PRE_SELL("pre_sell", "أوردرات تسليم")
}

data class OrderUiState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val error: String? = null,
    val cancellationError: String? = null,
    val selectedTab: OrderType = OrderType.PRE_SELL,
    val isPrinting: Boolean = false,
    val printStatus: String? = null
)

data class OrderDetailsUiState(
    val isLoading: Boolean = false,
    val order: Order? = null,
    val error: String? = null,
    val cancellationError: String? = null,
    val successMessage: String? = null,
    val isEditMode: Boolean = false,
    val editedQuantities: Map<String, Int> = emptyMap(),
    val deletedProductIds: Set<String> = emptySet(),
    val isUpdatingPrice: Boolean = false,
    val productPrices: Map<String, ProductPriceInfo> = emptyMap(),
    val loadingPriceForProducts: Set<String> = emptySet(),
    val rebateValue: String = "",
    val isVoidingInvoice: Boolean = false,
    val isVoidingBeforeEdit: Boolean = false
)
