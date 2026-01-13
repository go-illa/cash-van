package com.illa.cashvan.feature.orders.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.CreateOrderRequest
import com.illa.cashvan.feature.orders.data.model.CreateOrderResponse
import com.illa.cashvan.feature.orders.data.model.MerchantItem
import com.illa.cashvan.feature.orders.data.model.OngoingPlanResponse
import com.illa.cashvan.feature.orders.data.model.OrderData
import com.illa.cashvan.feature.orders.data.model.OrderItem
import com.illa.cashvan.feature.orders.data.model.PlanProduct
import com.illa.cashvan.feature.orders.domain.usecase.CreateOrderUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetOngoingPlanUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetPlanProductsUseCase
import com.illa.cashvan.feature.orders.domain.usecase.SearchMerchantsUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetOrderByIdUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetInvoiceContentUseCase
import com.illa.cashvan.feature.printer.HoneywellPrinterManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.illa.cashvan.feature.printer.CpclInvoiceFormatter
import java.nio.charset.Charset

data class CreateOrderUiState(
    val isLoading: Boolean = false,
    val currentPlan: OngoingPlanResponse? = null,
    val merchants: List<MerchantItem> = emptyList(),
    val products: List<PlanProduct> = emptyList(),
    val selectedMerchant: MerchantItem? = null,
    val selectedProducts: Map<String, Int> = emptyMap(),
    val merchantSearchQuery: String = "",
    val productSearchQuery: String = "",
    val isSearchingMerchants: Boolean = false,
    val isSearchingProducts: Boolean = false,
    val error: String? = null,
    val orderCreated: Boolean = false,
    val isPrinting: Boolean = false,
    val printStatus: String? = null,
    val invoiceText: String? = null
)

class CreateOrderViewModel(
    private val context: Context,
    private val getOngoingPlanUseCase: GetOngoingPlanUseCase,
    private val searchMerchantsUseCase: SearchMerchantsUseCase,
    private val getPlanProductsUseCase: GetPlanProductsUseCase,
    private val createOrderUseCase: CreateOrderUseCase,
    private val getOrderByIdUseCase: GetOrderByIdUseCase,
    private val getInvoiceContentUseCase: GetInvoiceContentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateOrderUiState())
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()

    private var merchantSearchJob: Job? = null
    private var productSearchJob: Job? = null
    private val printerManager: HoneywellPrinterManager by lazy {
        HoneywellPrinterManager(context)
    }

    init {
        loadOngoingPlan()
    }

    private fun loadOngoingPlan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = getOngoingPlanUseCase()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentPlan = result.data
                    )
                    // Load initial products
                    loadProducts()
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

    fun searchMerchants(query: String) {
        _uiState.value = _uiState.value.copy(merchantSearchQuery = query)

        // Cancel previous search
        merchantSearchJob?.cancel()

        merchantSearchJob = viewModelScope.launch {
            // Debounce search
            delay(300)

            _uiState.value = _uiState.value.copy(isSearchingMerchants = true)

            // Use empty string to fetch all merchants when query is empty
            val searchQuery = query.ifEmpty { "" }

            when (val result = searchMerchantsUseCase(searchQuery)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSearchingMerchants = false,
                        merchants = result.data.merchants
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSearchingMerchants = false,
                        error = result.message
                    )
                }
                is ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isSearchingMerchants = true)
                }
            }
        }
    }

    fun searchProducts(query: String) {
        _uiState.value = _uiState.value.copy(productSearchQuery = query)

        // Cancel previous search
        productSearchJob?.cancel()

        val planId = _uiState.value.currentPlan?.id?.toString() ?: return

        productSearchJob = viewModelScope.launch {
            // Debounce search
            delay(300)

            _uiState.value = _uiState.value.copy(isSearchingProducts = true)

            when (val result = getPlanProductsUseCase(planId, query.ifEmpty { null })) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSearchingProducts = false,
                        products = result.data.plan_products
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSearchingProducts = false,
                        error = result.message
                    )
                }
                is ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isSearchingProducts = true)
                }
            }
        }
    }

    private fun loadProducts() {
        val planId = _uiState.value.currentPlan?.id?.toString() ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearchingProducts = true)

            when (val result = getPlanProductsUseCase(planId, null)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSearchingProducts = false,
                        products = result.data.plan_products
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSearchingProducts = false,
                        error = result.message
                    )
                }
                is ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isSearchingProducts = true)
                }
            }
        }
    }

    fun selectMerchant(merchant: MerchantItem) {
        _uiState.value = _uiState.value.copy(
            selectedMerchant = merchant,
            merchantSearchQuery = merchant.name
        )
    }

    fun clearMerchant() {
        _uiState.value = _uiState.value.copy(
            selectedMerchant = null
        )
    }

    fun addProductToOrder(planProductId: String, quantity: Int) {
        val currentProducts = linkedMapOf<String, Int>()
        val currentQuantity = _uiState.value.selectedProducts[planProductId] ?: 0
        currentProducts[planProductId] = currentQuantity + quantity
        _uiState.value.selectedProducts.forEach { (id, qty) ->
            if (id != planProductId) {
                currentProducts[id] = qty
            }
        }

        _uiState.value = _uiState.value.copy(selectedProducts = currentProducts)
    }

    fun updateProductQuantity(planProductId: String, quantity: Int) {
        val currentProducts = _uiState.value.selectedProducts.toMutableMap()
        if (quantity <= 0) {
            currentProducts.remove(planProductId)
        } else {
            currentProducts[planProductId] = quantity
        }

        _uiState.value = _uiState.value.copy(selectedProducts = currentProducts)
    }

    fun removeProduct(planProductId: String) {
        val currentProducts = _uiState.value.selectedProducts.toMutableMap()
        currentProducts.remove(planProductId)

        _uiState.value = _uiState.value.copy(selectedProducts = currentProducts)
    }

    fun createOrder() {
        val state = _uiState.value

        if (state.currentPlan == null || state.selectedMerchant == null || state.selectedProducts.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please select merchant and add products")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val orderItems = state.selectedProducts.map { (planProductId, quantity) ->
                OrderItem(
                    plan_product_id = planProductId,
                    sold_quantity = quantity.toString()
                )
            }

            val request = CreateOrderRequest(
                order = OrderData(
                    plan_id = state.currentPlan.id ?: "",
                    merchant_id = state.selectedMerchant.id,
                    order_items = orderItems
                )
            )

            when (val result = createOrderUseCase(request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        orderCreated = true
                    )

                    // Print invoice after successful order creation
                    printInvoice(result.data)
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetOrderCreated() {
        _uiState.value = _uiState.value.copy(orderCreated = false)
    }

    fun resetState() {
        _uiState.value = _uiState.value.copy(
            merchants = emptyList(),
            products = _uiState.value.products, // Keep products as they are loaded with plan
            selectedMerchant = null,
            selectedProducts = emptyMap(),
            merchantSearchQuery = "",
            productSearchQuery = "",
            error = null
        )
    }

    /**
     * Print invoice after order creation
     * Fetches the invoice from the API and prints it
     */
    private fun printInvoice(order: CreateOrderResponse) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isPrinting = true, printStatus = "جاري جلب الفاتورة...")

                val orderResult = getOrderByIdUseCase(order.id)
                if (orderResult !is ApiResult.Success) {
                    throw Exception("فشل جلب تفاصيل الطلب: ")
                }

                val attachment = orderResult.data.invoice_attachment
                    ?: throw Exception("لم يتم العثور على مرفق فاتورة")

                val invoiceUrl = attachment.url

                _uiState.value = _uiState.value.copy(printStatus = "جاري تحميل الفاتورة...")

                val invoiceResult = getInvoiceContentUseCase(invoiceUrl)
                if (invoiceResult !is ApiResult.Success) {
                    throw Exception("فشل تحميل الفاتورة: ")
                }

                val invoiceText = invoiceResult.data

                _uiState.value = _uiState.value.copy(printStatus = "جاري تهيئة الفاتورة للطباعة...")

                val cpclBytes = CpclInvoiceFormatter.formatInvoiceAsCpclBytes(invoiceText)

                Log.d("CreateOrderVM", "CPCL prepared – ${cpclBytes.size} bytes")

                _uiState.value = _uiState.value.copy(printStatus = "جاري الطباعة...")

                val printResult = printerManager.printInvoice(cpclBytes)

                if (printResult.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printStatus = "تمت طباعة الفاتورة بنجاح ✓"
                    )
                } else {
                    throw printResult.exceptionOrNull() ?: Exception("فشل الطباعة – سبب غير معروف")
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    printStatus = "خطأ أثناء الطباعة: ${e.message}"
                )
                Log.e("CreateOrderVM", "Printing failed", e)
            }
        }
    }






    /**
     * Clear print status message
     */
    fun clearPrintStatus() {
        _uiState.value = _uiState.value.copy(printStatus = null)
    }





    /**
     * Cleanup resources
     */
    override fun onCleared() {
        super.onCleared()
        printerManager.disconnect()
    }
}