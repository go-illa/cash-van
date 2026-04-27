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
import com.illa.cashvan.feature.orders.domain.usecase.GetCashVanProductTotalPriceUseCase
import com.illa.cashvan.feature.printer.HoneywellPrinterManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.illa.cashvan.feature.printer.CpclInvoiceFormatter

data class CreateOrderUiState(
    val isLoading: Boolean = false,
    val currentPlan: OngoingPlanResponse? = null,
    val merchants: List<MerchantItem> = emptyList(),
    val merchantPage: Int = 1,
    val hasMoreMerchants: Boolean = true,
    val isLoadingMoreMerchants: Boolean = false,
    val products: List<PlanProduct> = emptyList(),
    val allProducts: List<PlanProduct> = emptyList(),
    val productPage: Int = 1,
    val hasMoreProducts: Boolean = true,
    val isLoadingMoreProducts: Boolean = false,
    val selectedMerchant: MerchantItem? = null,
    val selectedProducts: Map<String, Int> = emptyMap(),
    val merchantSearchQuery: String = "",
    val productSearchQuery: String = "",
    val isSearchingMerchants: Boolean = false,
    val isSearchingProducts: Boolean = false,
    val error: String? = null,
    val orderCreated: Boolean = false,
    val orderCreationError: String? = null,
    val isPrinting: Boolean = false,
    val printStatus: String? = null,
    val invoiceText: String? = null,
    val productPrices: Map<String, ProductPriceInfo> = emptyMap(),
    val loadingPriceForProducts: Set<String> = emptySet(),
    val previewProductPrice: ProductPriceInfo? = null,
    val isLoadingPreviewPrice: Boolean = false,
    val rebateValue: String = ""
)

class CreateOrderViewModel(
    private val context: Context,
    private val getOngoingPlanUseCase: GetOngoingPlanUseCase,
    private val searchMerchantsUseCase: SearchMerchantsUseCase,
    private val getPlanProductsUseCase: GetPlanProductsUseCase,
    private val createOrderUseCase: CreateOrderUseCase,
    private val getOrderByIdUseCase: GetOrderByIdUseCase,
    private val getInvoiceContentUseCase: GetInvoiceContentUseCase,
    private val getCashVanProductTotalPriceUseCase: GetCashVanProductTotalPriceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateOrderUiState())
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()

    private var merchantSearchJob: Job? = null
    private var productSearchJob: Job? = null
    private var previewPriceJob: Job? = null
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
                    val plan = result.data
                    if (plan?.id == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "لا يوجد خطة جارية حالياً"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, currentPlan = plan)
                        loadProducts(_uiState.value.selectedMerchant?.price_tier)
                    }
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
        _uiState.value = _uiState.value.copy(
            merchantSearchQuery = query,
            merchantPage = 1,
            hasMoreMerchants = true
        )

        merchantSearchJob?.cancel()

        merchantSearchJob = viewModelScope.launch {
            delay(300)

            _uiState.value = _uiState.value.copy(isSearchingMerchants = true)

            try {
                when (val result = searchMerchantsUseCase(query, page = 1)) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isSearchingMerchants = false,
                            merchants = result.data.merchants,
                            hasMoreMerchants = result.data.merchants.size >= 20
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
            } finally {
                _uiState.value = _uiState.value.copy(isSearchingMerchants = false)
            }
        }
    }

    fun loadMoreMerchants() {
        val state = _uiState.value
        if (!state.hasMoreMerchants || state.isLoadingMoreMerchants || state.isSearchingMerchants) return

        val nextPage = state.merchantPage + 1
        _uiState.value = state.copy(isLoadingMoreMerchants = true)

        viewModelScope.launch {
            try {
                when (val result = searchMerchantsUseCase(state.merchantSearchQuery, page = nextPage)) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoadingMoreMerchants = false,
                            merchants = _uiState.value.merchants + result.data.merchants,
                            merchantPage = nextPage,
                            hasMoreMerchants = result.data.merchants.size >= 20
                        )
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(isLoadingMoreMerchants = false)
                    }
                    is ApiResult.Loading -> {}
                }
            } finally {
                _uiState.value = _uiState.value.copy(isLoadingMoreMerchants = false)
            }
        }
    }

    fun searchProducts(query: String) {
        _uiState.value = _uiState.value.copy(
            productSearchQuery = query,
            productPage = 1,
            hasMoreProducts = true
        )
        productSearchJob?.cancel()
        val planId = _uiState.value.currentPlan?.id?.toString() ?: return
        val priceTier = _uiState.value.selectedMerchant?.price_tier
        _uiState.value = _uiState.value.copy(isSearchingProducts = true)

        productSearchJob = viewModelScope.launch {
            delay(300)

            try {
                when (val result = getPlanProductsUseCase(planId, query.ifEmpty { null }, priceTier, page = 1)) {
                    is ApiResult.Success -> {
                        val newProducts = result.data.plan_products
                        val mergedAllProducts = (_uiState.value.allProducts + newProducts).distinctBy { it.id }
                        _uiState.value = _uiState.value.copy(
                            isSearchingProducts = false,
                            products = newProducts,
                            allProducts = mergedAllProducts,
                            hasMoreProducts = newProducts.size >= 20
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
            } finally {
                _uiState.value = _uiState.value.copy(isSearchingProducts = false)
            }
        }
    }

    fun loadMoreProducts() {
        val state = _uiState.value
        if (!state.hasMoreProducts || state.isLoadingMoreProducts || state.isSearchingProducts) return

        val nextPage = state.productPage + 1
        val planId = state.currentPlan?.id?.toString() ?: return
        val priceTier = state.selectedMerchant?.price_tier

        _uiState.value = state.copy(isLoadingMoreProducts = true)

        viewModelScope.launch {
            try {
                when (val result = getPlanProductsUseCase(planId, state.productSearchQuery.ifEmpty { null }, priceTier, page = nextPage)) {
                    is ApiResult.Success -> {
                        val newProducts = result.data.plan_products
                        val mergedAll = (_uiState.value.allProducts + newProducts).distinctBy { it.id }
                        _uiState.value = _uiState.value.copy(
                            isLoadingMoreProducts = false,
                            products = _uiState.value.products + newProducts,
                            allProducts = mergedAll,
                            productPage = nextPage,
                            hasMoreProducts = newProducts.size >= 20
                        )
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(isLoadingMoreProducts = false)
                    }
                    is ApiResult.Loading -> {}
                }
            } finally {
                _uiState.value = _uiState.value.copy(isLoadingMoreProducts = false)
            }
        }
    }

    fun refreshProducts() {
        loadProducts(_uiState.value.selectedMerchant?.price_tier)
    }

    private fun loadProducts(priceTier: String? = null) {
        val planId = _uiState.value.currentPlan?.id?.toString() ?: return

        _uiState.value = _uiState.value.copy(productPage = 1, hasMoreProducts = true, isSearchingProducts = true)

        viewModelScope.launch {
            when (val result = getPlanProductsUseCase(planId, null, priceTier, page = 1)) {
                is ApiResult.Success -> {
                    val products = result.data.plan_products
                    _uiState.value = _uiState.value.copy(
                        isSearchingProducts = false,
                        products = products,
                        allProducts = products,
                        hasMoreProducts = products.size >= 20
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
            merchantSearchQuery = merchant.displayName
        )
        loadProducts(merchant.price_tier)
    }

    fun clearMerchant() {
        _uiState.value = _uiState.value.copy(
            selectedMerchant = null,
            selectedProducts = emptyMap(),
            productPrices = emptyMap(),
            productSearchQuery = "",
            products = emptyList()
        )
        loadProducts(priceTier = null)
    }

    fun fetchPreviewPrice(planProductId: String, quantity: Int) {
        val planId = _uiState.value.currentPlan?.id ?: return
        val merchantId = _uiState.value.selectedMerchant?.id ?: return

        previewPriceJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoadingPreviewPrice = true, previewProductPrice = null)

        previewPriceJob = viewModelScope.launch {
            delay(300)
            when (val result = getCashVanProductTotalPriceUseCase(
                planId = planId,
                productId = planProductId,
                merchantId = merchantId,
                quantity = quantity
            )) {
                is ApiResult.Success -> {
                    val priceResponse = result.data
                    val priceInfo = if (priceResponse.unit != null && priceResponse.total != null) {
                        ProductPriceInfo(
                            basePrice = priceResponse.unit.base_price ?: 0.0,
                            finalPrice = priceResponse.unit.final_price ?: 0.0,
                            discountAmount = priceResponse.unit.discount_amount ?: 0.0,
                            vatAmount = priceResponse.unit.vat_amount ?: 0.0,
                            totalPrice = priceResponse.total.final_price ?: 0.0,
                            vatPercentage = priceResponse.vat_percentage ?: 0.0
                        )
                    } else {
                        ProductPriceInfo(
                            basePrice = priceResponse.base_price ?: 0.0,
                            finalPrice = priceResponse.final_price ?: 0.0,
                            discountAmount = priceResponse.total_discount ?: 0.0,
                            vatAmount = priceResponse.total_vat ?: 0.0,
                            totalPrice = priceResponse.total_price ?: 0.0,
                            vatPercentage = priceResponse.vat_percentage ?: 0.0
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        previewProductPrice = priceInfo,
                        isLoadingPreviewPrice = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoadingPreviewPrice = false)
                }
                is ApiResult.Loading -> {}
            }
        }
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

        _uiState.value = _uiState.value.copy(
            selectedProducts = currentProducts,
            previewProductPrice = null,
            isLoadingPreviewPrice = false
        )

        if (_uiState.value.selectedMerchant != null) {
            calculateProductPrice(planProductId, currentProducts[planProductId] ?: quantity)
        }
    }

    fun updateProductQuantity(planProductId: String, quantity: Int) {
        val currentProducts = _uiState.value.selectedProducts.toMutableMap()
        if (quantity <= 0) {
            currentProducts.remove(planProductId)
        } else {
            currentProducts[planProductId] = quantity
        }

        _uiState.value = _uiState.value.copy(selectedProducts = currentProducts)

        if (quantity > 0 && _uiState.value.selectedMerchant != null) {
            calculateProductPrice(planProductId, quantity)
        }
    }

    fun removeProduct(planProductId: String) {
        val currentProducts = _uiState.value.selectedProducts.toMutableMap()
        currentProducts.remove(planProductId)

        _uiState.value = _uiState.value.copy(selectedProducts = currentProducts)
    }

    private fun calculateProductPrice(planProductId: String, quantity: Int) {
        viewModelScope.launch {
            val currentPlan = _uiState.value.currentPlan ?: return@launch
            val selectedMerchant = _uiState.value.selectedMerchant ?: return@launch
            val planId = currentPlan.id?.toString() ?: return@launch

            val loadingProducts = _uiState.value.loadingPriceForProducts.toMutableSet()
            loadingProducts.add(planProductId)
            _uiState.value = _uiState.value.copy(
                loadingPriceForProducts = loadingProducts
            )

            when (val result = getCashVanProductTotalPriceUseCase(
                planId = planId,
                productId = planProductId,
                merchantId = selectedMerchant.id,
                quantity = quantity
            )) {
                is ApiResult.Success -> {
                    val priceResponse = result.data

                    val priceInfo = if (priceResponse.unit != null && priceResponse.total != null) {
                        ProductPriceInfo(
                            basePrice = priceResponse.unit.base_price ?: 0.0,
                            finalPrice = priceResponse.unit.final_price ?: 0.0,
                            discountAmount = priceResponse.unit.discount_amount ?: 0.0,
                            vatAmount = priceResponse.unit.vat_amount ?: 0.0,
                            totalPrice = priceResponse.total.final_price ?: 0.0,
                            vatPercentage = priceResponse.vat_percentage ?: 0.0
                        )
                    } else {
                        ProductPriceInfo(
                            basePrice = priceResponse.base_price ?: 0.0,
                            finalPrice = priceResponse.final_price ?: 0.0,
                            discountAmount = priceResponse.total_discount ?: 0.0,
                            vatAmount = priceResponse.total_vat ?: 0.0,
                            totalPrice = priceResponse.total_price ?: 0.0,
                            vatPercentage = priceResponse.vat_percentage ?: 0.0
                        )
                    }

                    val updatedPrices = _uiState.value.productPrices.toMutableMap()
                    updatedPrices[planProductId] = priceInfo

                    val updatedLoadingProducts = _uiState.value.loadingPriceForProducts.toMutableSet()
                    updatedLoadingProducts.remove(planProductId)

                    _uiState.value = _uiState.value.copy(
                        productPrices = updatedPrices,
                        loadingPriceForProducts = updatedLoadingProducts
                    )
                }
                is ApiResult.Error -> {
                    val updatedLoadingProducts = _uiState.value.loadingPriceForProducts.toMutableSet()
                    updatedLoadingProducts.remove(planProductId)
                    _uiState.value = _uiState.value.copy(
                        loadingPriceForProducts = updatedLoadingProducts
                    )
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun createOrder() {
        val state = _uiState.value

        if (state.currentPlan == null || state.selectedMerchant == null || state.selectedProducts.isEmpty()) return

        viewModelScope.launch {
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
                    rebate_value = state.rebateValue.toDoubleOrNull(),
                    order_items = orderItems
                )
            )

            when (val result = createOrderUseCase(request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(orderCreated = true)
                    printInvoice(result.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(orderCreationError = result.message)
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun updateRebateValue(value: String) {
        if (value.isEmpty() || value.toDoubleOrNull() != null) {
            _uiState.value = _uiState.value.copy(rebateValue = value)
        }
    }

    fun retryLoadPlan() {
        loadOngoingPlan()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearOrderCreationError() {
        _uiState.value = _uiState.value.copy(orderCreationError = null)
    }

    fun resetOrderCreated() {
        _uiState.value = _uiState.value.copy(orderCreated = false)
    }

    fun resetState() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            merchants = emptyList(),
            products = emptyList(),
            allProducts = emptyList(),
            currentPlan = null,
            selectedMerchant = null,
            selectedProducts = emptyMap(),
            productPrices = emptyMap(),
            merchantSearchQuery = "",
            productSearchQuery = "",
            error = null
        )
        loadOngoingPlan()
    }

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
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        printerManager.disconnect()
    }
}
