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
import com.illa.cashvan.feature.printer.HoneywellPrinterManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
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
    private val createOrderUseCase: CreateOrderUseCase
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
     * Prints the SAMPLE_INVOICE.txt file
     */
    private fun printInvoice(order: CreateOrderResponse) {
        viewModelScope.launch {
            try {
                Log.d("CreateOrderVM", "========================================")
                Log.d("CreateOrderVM", "Starting print process for order ${order.id}")
                Log.d("CreateOrderVM", "========================================")

                _uiState.value = _uiState.value.copy(
                    isPrinting = true,
                    printStatus = "Connecting to printer..."
                )

                // First, test simple print (using printInvoice which handles connection)
                Log.d("CreateOrderVM", "Attempting simple test print")
                val testPrint = printerManager.printInvoice("TEST PRINT - Order ${order.id}\n\n")

                if (testPrint.isFailure) {
                    Log.e("CreateOrderVM", "Test print failed: ${testPrint.exceptionOrNull()?.message}")
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printStatus = "Printer connection failed: ${testPrint.exceptionOrNull()?.message}"
                    )
                    return@launch
                }

                Log.d("CreateOrderVM", "Test print successful, loading sample invoice")

                // Load sample invoice text from assets
                val invoiceText = try {
                    context.assets.open("SAMPLE_INVOICE.txt")
                        .bufferedReader(Charset.forName("UTF-8"))
                        .use { it.readText() }
                } catch (e: Exception) {
                    Log.e("CreateOrderVM", "Failed to load SAMPLE_INVOICE.txt", e)
                    "=== ERROR ===\nFailed to load invoice file\n${e.message}\n============\n"
                }

                Log.d("CreateOrderVM", "Loaded invoice, length: ${invoiceText.length} characters")
                Log.d("CreateOrderVM", "Invoice preview: ${invoiceText.take(100)}")

                // Use CPCL with CENTER alignment - force each line to be separate
                Log.d("CreateOrderVM", "Generating CPCL with centered separate lines...")

                val lines = invoiceText.lines() // Keep ALL lines including blank ones
                val labelHeight = lines.size * 35 + 200 // Height for spacing

                val cpclCommands = buildString {
                    // CPCL Header
                    append("! 0 200 200 $labelHeight 1\r\n")

                    // Set encoding for Arabic
                    append("ENCODING UTF-8\r\n")

                    // Set magnification to 0 0 (normal size)
                    append("SETMAG 0 0\r\n")

                    var yPos = 15
                    for ((index, line) in lines.withIndex()) {
                        // CENTER each line individually
                        append("CENTER\r\n")

                        // Use font 7 (monospace) to preserve spacing
                        append("TEXT 7 0 0 $yPos $line\r\n")

                        // Reset to LEFT after each centered line
                        // This forces the printer to process each line separately
                        append("LEFT\r\n")

                        yPos += 30  // Spacing between lines

                        Log.d("CreateOrderVM", "Line $index (yPos=$yPos): ${line.take(50)}")
                    }

                    // Print command
                    append("PRINT\r\n")
                }

                Log.d("CreateOrderVM", "CPCL length: ${cpclCommands.length}")
                Log.d("CreateOrderVM", "Total lines: ${lines.size}")
                Log.d("CreateOrderVM", "Label height: $labelHeight")

                val printResult = printerManager.printInvoice(cpclCommands)

                if (printResult.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printStatus = "Invoice printed successfully!"
                    )
                    Log.d("CreateOrderVM", "Invoice printed successfully!")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printStatus = "Print failed: ${printResult.exceptionOrNull()?.message}"
                    )
                    Log.e("CreateOrderVM", "Print failed: ${printResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    printStatus = "Error: ${e.message}"
                )
                Log.e("CreateOrderVM", "Exception during print process", e)
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