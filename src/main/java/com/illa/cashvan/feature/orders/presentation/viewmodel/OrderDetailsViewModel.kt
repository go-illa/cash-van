package com.illa.cashvan.feature.orders.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.SubmitOrderItem
import com.illa.cashvan.feature.orders.data.model.UpdateOrderData
import com.illa.cashvan.feature.orders.data.model.UpdateOrderRequest
import com.illa.cashvan.feature.orders.domain.usecase.GetCashVanProductTotalPriceUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetOrderByIdUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetProductTotalPriceUseCase
import com.illa.cashvan.feature.orders.domain.usecase.UpdateOrderUseCase
import com.illa.cashvan.feature.orders.domain.usecase.VoidInvoiceUseCase
import com.illa.cashvan.feature.orders.presentation.mapper.ProductPriceInfo
import com.illa.cashvan.feature.orders.presentation.mapper.toProductPriceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderDetailsViewModel(
    private val getOrderByIdUseCase: GetOrderByIdUseCase,
    private val updateOrderUseCase: UpdateOrderUseCase,
    private val getProductTotalPriceUseCase: GetProductTotalPriceUseCase,
    private val getCashVanProductTotalPriceUseCase: GetCashVanProductTotalPriceUseCase,
    private val voidInvoiceUseCase: VoidInvoiceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailsUiState())
    val uiState: StateFlow<OrderDetailsUiState> = _uiState.asStateFlow()

    fun loadOrderById(orderId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                isEditMode = false,
                editedQuantities = emptyMap(),
                deletedProductIds = emptySet(),
                rebateValue = ""
            )
            when (val result = getOrderByIdUseCase(orderId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, order = result.data)
                    initializeProductPrices()
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                is ApiResult.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
            }
        }
    }

    fun initializeProductPrices() {
        val currentOrder = _uiState.value.order ?: return
        val initialPrices = currentOrder.order_plan_products?.associate { orderPlanProduct ->
            val planProductId = orderPlanProduct.plan_product_id ?: ""
            val totalPriceDetails = orderPlanProduct.total_price_details
            val quantity = orderPlanProduct.sold_quantity

            val priceInfo = if (totalPriceDetails != null) {
                ProductPriceInfo(
                    basePrice = totalPriceDetails.unit?.base_price ?: 0.0,
                    finalPrice = totalPriceDetails.unit?.final_price ?: 0.0,
                    discountAmount = totalPriceDetails.unit?.discount_amount ?: 0.0,
                    vatAmount = totalPriceDetails.unit?.vat_amount ?: 0.0,
                    totalPrice = totalPriceDetails.total?.final_price ?: 0.0,
                    vatPercentage = totalPriceDetails.vat_percentage ?: 0.0
                )
            } else {
                val priceDetails = orderPlanProduct.plan_product_price?.price_details
                ProductPriceInfo(
                    basePrice = orderPlanProduct.plan_product_price?.base_price?.toDoubleOrNull() ?: 0.0,
                    finalPrice = priceDetails?.final_price ?: 0.0,
                    discountAmount = priceDetails?.discount_amount ?: 0.0,
                    vatAmount = priceDetails?.vat_amount ?: 0.0,
                    totalPrice = (priceDetails?.final_price ?: 0.0) * quantity,
                    vatPercentage = orderPlanProduct.plan_product_price?.vat_percentage ?: 0.0
                )
            }
            planProductId to priceInfo
        }?.toMap() ?: emptyMap()

        _uiState.value = _uiState.value.copy(productPrices = initialPrices)
    }

    fun enterEditMode() {
        val currentOrder = _uiState.value.order ?: return
        val initialQuantities = currentOrder.order_plan_products?.associate {
            (it.plan_product_id ?: "") to it.sold_quantity
        } ?: emptyMap()
        _uiState.value = _uiState.value.copy(
            isEditMode = true,
            editedQuantities = initialQuantities,
            deletedProductIds = emptySet()
        )
    }

    fun exitEditMode() {
        _uiState.value = _uiState.value.copy(
            isEditMode = false,
            editedQuantities = emptyMap(),
            deletedProductIds = emptySet()
        )
    }

    fun updateProductQuantity(planProductId: String, newQuantity: Int) {
        val currentQuantities = _uiState.value.editedQuantities.toMutableMap()
        currentQuantities[planProductId] = newQuantity
        _uiState.value = _uiState.value.copy(editedQuantities = currentQuantities)
        calculateProductPrice(planProductId, newQuantity)
    }

    fun updateRebateValue(value: String) {
        if (value.isEmpty() || value.toDoubleOrNull() != null) {
            _uiState.value = _uiState.value.copy(rebateValue = value)
        }
    }

    private fun calculateProductPrice(planProductId: String, quantity: Int) {
        viewModelScope.launch {
            val currentOrder = _uiState.value.order ?: return@launch
            val orderPlanProduct = currentOrder.order_plan_products?.find {
                it.plan_product_id == planProductId
            } ?: return@launch

            val productId = orderPlanProduct.plan_product_id ?: return@launch
            val planId = currentOrder.plan_id ?: return@launch

            val loadingProducts = _uiState.value.loadingPriceForProducts.toMutableSet()
            loadingProducts.add(planProductId)
            _uiState.value = _uiState.value.copy(loadingPriceForProducts = loadingProducts)

            val result = if (currentOrder.order_type == "cash_van") {
                val merchantId = currentOrder.merchant?.id ?: return@launch
                getCashVanProductTotalPriceUseCase(planId, productId, merchantId, quantity)
            } else {
                getProductTotalPriceUseCase(planId, productId, currentOrder.id, quantity)
            }

            val updatedLoadingProducts = _uiState.value.loadingPriceForProducts.toMutableSet()
            updatedLoadingProducts.remove(planProductId)

            when (result) {
                is ApiResult.Success -> {
                    val updatedPrices = _uiState.value.productPrices.toMutableMap()
                    updatedPrices[planProductId] = result.data.toProductPriceInfo()
                    _uiState.value = _uiState.value.copy(
                        productPrices = updatedPrices,
                        loadingPriceForProducts = updatedLoadingProducts
                    )
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    loadingPriceForProducts = updatedLoadingProducts
                )
                is ApiResult.Loading -> {}
            }
        }
    }

    fun deleteProductImmediately(
        planProductId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentOrder = _uiState.value.order ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val request = UpdateOrderRequest(
                order = UpdateOrderData(
                    order_items = listOf(
                        SubmitOrderItem(
                            plan_product_id = planProductId,
                            sold_quantity = 0,
                            _destroy = true
                        )
                    )
                )
            )

            when (val result = updateOrderUseCase(currentOrder.id, request)) {
                is ApiResult.Success -> {
                    loadOrderById(currentOrder.id)
                    _uiState.value = _uiState.value.copy(successMessage = "تم حذف المنتج بنجاح")
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                    onError(result.message)
                }
                else -> {}
            }
        }
    }

    fun saveEditedOrder(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val currentOrder = _uiState.value.order ?: return@launch
            val editedQuantities = _uiState.value.editedQuantities
            val deletedProductIds = _uiState.value.deletedProductIds

            val orderItems = editedQuantities
                .filter { (planProductId, _) -> planProductId !in deletedProductIds }
                .filter { (_, quantity) -> quantity > 0 }
                .map { (planProductId, quantity) ->
                    SubmitOrderItem(plan_product_id = planProductId, sold_quantity = quantity)
                }

            val request = UpdateOrderRequest(order = UpdateOrderData(order_items = orderItems))

            when (val result = updateOrderUseCase(currentOrder.id, request)) {
                is ApiResult.Success -> {
                    loadOrderById(currentOrder.id)
                    exitEditMode()
                    _uiState.value = _uiState.value.copy(successMessage = "تم حفظ التعديلات بنجاح")
                    onSuccess()
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
                is ApiResult.Loading -> {}
            }
        }
    }

    fun voidInvoice(orderId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVoidingInvoice = true, error = null)
            when (val result = voidInvoiceUseCase(orderId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isVoidingInvoice = false,
                        successMessage = "تم إلغاء الفاتورة بنجاح"
                    )
                    loadOrderById(orderId)
                    onSuccess()
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isVoidingInvoice = false,
                    error = result.message
                )
                is ApiResult.Loading -> {}
            }
        }
    }

    fun voidInvoiceAndEnterEditMode(orderId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVoidingBeforeEdit = true, error = null)
            when (val result = voidInvoiceUseCase(orderId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isVoidingBeforeEdit = false)
                    enterEditMode()
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isVoidingBeforeEdit = false,
                    error = result.message
                )
                is ApiResult.Loading -> {}
            }
        }
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearCancellationError() {
        _uiState.value = _uiState.value.copy(cancellationError = null)
    }
}
