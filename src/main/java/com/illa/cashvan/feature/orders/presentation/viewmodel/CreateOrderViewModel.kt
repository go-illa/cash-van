package com.illa.cashvan.feature.orders.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.location.LocationService
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.merchant.domain.usecase.UpdateMerchantUseCase
import com.illa.cashvan.feature.orders.data.model.CreateOrderRequest
import com.illa.cashvan.feature.orders.data.model.CreateOrderResponse
import com.illa.cashvan.feature.orders.data.model.OrderData
import com.illa.cashvan.feature.orders.data.model.OrderItem
import com.illa.cashvan.feature.orders.domain.usecase.CreateOrderUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetCashVanProductTotalPriceUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetInvoiceContentUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetOngoingPlanUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetOrderByIdUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetPlanProductsUseCase
import com.illa.cashvan.feature.orders.domain.usecase.SearchMerchantsUseCase
import com.illa.cashvan.feature.orders.presentation.mapper.toProductPriceInfo
import com.illa.cashvan.feature.printer.CpclInvoiceFormatter
import com.illa.cashvan.feature.printer.HoneywellPrinterManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateOrderViewModel(
    private val context: Context,
    private val locationService: LocationService,
    private val getOngoingPlanUseCase: GetOngoingPlanUseCase,
    private val searchMerchantsUseCase: SearchMerchantsUseCase,
    private val getPlanProductsUseCase: GetPlanProductsUseCase,
    private val createOrderUseCase: CreateOrderUseCase,
    private val getOrderByIdUseCase: GetOrderByIdUseCase,
    private val getInvoiceContentUseCase: GetInvoiceContentUseCase,
    private val getCashVanProductTotalPriceUseCase: GetCashVanProductTotalPriceUseCase,
    private val updateMerchantUseCase: UpdateMerchantUseCase,
    private val printerManager: HoneywellPrinterManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateOrderUiState())
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()

    private var merchantSearchJob: Job? = null
    private var productSearchJob: Job? = null
    private var previewPriceJob: Job? = null

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
                        _uiState.value = _uiState.value.copy(isLoading = false, noPlanFound = true)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, currentPlan = plan)
                        loadProducts(_uiState.value.selectedMerchant?.price_tier)
                    }
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                is ApiResult.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
            }
        }
    }

    fun searchMerchants(query: String) {
        val lat = _uiState.value.userLatitude ?: return
        val lon = _uiState.value.userLongitude ?: return

        _uiState.value = _uiState.value.copy(merchantSearchQuery = query, merchantPage = 1, hasMoreMerchants = true)
        merchantSearchJob?.cancel()

        merchantSearchJob = viewModelScope.launch {
            delay(300)
            _uiState.value = _uiState.value.copy(isSearchingMerchants = true)
            try {
                when (val result = searchMerchantsUseCase(query, lat, lon, page = 1)) {
                    is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                        isSearchingMerchants = false,
                        merchants = result.data.merchants,
                        hasMoreMerchants = result.data.merchants.size >= 20
                    )
                    is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                        isSearchingMerchants = false,
                        error = result.message
                    )
                    is ApiResult.Loading -> _uiState.value = _uiState.value.copy(isSearchingMerchants = true)
                }
            } finally {
                _uiState.value = _uiState.value.copy(isSearchingMerchants = false)
            }
        }
    }

    fun loadMoreMerchants() {
        val state = _uiState.value
        if (!state.hasMoreMerchants || state.isLoadingMoreMerchants || state.isSearchingMerchants) return
        val lat = state.userLatitude ?: return
        val lon = state.userLongitude ?: return
        val nextPage = state.merchantPage + 1
        _uiState.value = state.copy(isLoadingMoreMerchants = true)

        viewModelScope.launch {
            try {
                when (val result = searchMerchantsUseCase(state.merchantSearchQuery, lat, lon, page = nextPage)) {
                    is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                        isLoadingMoreMerchants = false,
                        merchants = _uiState.value.merchants + result.data.merchants,
                        merchantPage = nextPage,
                        hasMoreMerchants = result.data.merchants.size >= 20
                    )
                    is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoadingMoreMerchants = false)
                    is ApiResult.Loading -> {}
                }
            } finally {
                _uiState.value = _uiState.value.copy(isLoadingMoreMerchants = false)
            }
        }
    }

    fun searchProducts(query: String) {
        _uiState.value = _uiState.value.copy(productSearchQuery = query, productPage = 1, hasMoreProducts = true)
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
                    is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                        isSearchingProducts = false,
                        error = result.message
                    )
                    is ApiResult.Loading -> _uiState.value = _uiState.value.copy(isSearchingProducts = true)
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
                    is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoadingMoreProducts = false)
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
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isSearchingProducts = false,
                    error = result.message
                )
                is ApiResult.Loading -> _uiState.value = _uiState.value.copy(isSearchingProducts = true)
            }
        }
    }

    fun selectMerchant(merchant: com.illa.cashvan.feature.orders.data.model.MerchantItem) {
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
            when (val result = getCashVanProductTotalPriceUseCase(planId, planProductId, merchantId, quantity)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    previewProductPrice = result.data.toProductPriceInfo(),
                    isLoadingPreviewPrice = false
                )
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoadingPreviewPrice = false)
                is ApiResult.Loading -> {}
            }
        }
    }

    fun addProductToOrder(planProductId: String, quantity: Int) {
        val currentProducts = linkedMapOf<String, Int>()
        val currentQuantity = _uiState.value.selectedProducts[planProductId] ?: 0
        currentProducts[planProductId] = currentQuantity + quantity
        _uiState.value.selectedProducts.forEach { (id, qty) ->
            if (id != planProductId) currentProducts[id] = qty
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
        if (quantity <= 0) currentProducts.remove(planProductId)
        else currentProducts[planProductId] = quantity
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
            _uiState.value = _uiState.value.copy(loadingPriceForProducts = loadingProducts)

            val updatedLoadingProducts = _uiState.value.loadingPriceForProducts.toMutableSet()
            updatedLoadingProducts.remove(planProductId)

            when (val result = getCashVanProductTotalPriceUseCase(planId, planProductId, selectedMerchant.id, quantity)) {
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

    fun createOrder() {
        val state = _uiState.value
        if (state.currentPlan == null || state.selectedMerchant == null || state.selectedProducts.isEmpty()) return

        if (state.paymentType != null && state.selectedMerchant.phone_number.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(orderCreationError = "رقم هاتف التاجر مطلوب لإتمام طلب الكاش")
            return
        }

        viewModelScope.launch {
            val orderItems = state.selectedProducts.map { (planProductId, quantity) ->
                OrderItem(plan_product_id = planProductId, sold_quantity = quantity.toString())
            }

            val request = CreateOrderRequest(
                order = OrderData(
                    plan_id = state.currentPlan.id ?: "",
                    merchant_id = state.selectedMerchant.id,
                    rebate_value = state.rebateValue.toDoubleOrNull(),
                    order_items = orderItems,
                    latitude = state.userLatitude?.toString(),
                    longitude = state.userLongitude?.toString(),
                    payment_type = state.paymentType
                )
            )

            when (val result = createOrderUseCase(request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(orderCreated = true)
                    printInvoice(result.data)
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(orderCreationError = result.message)
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

    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGettingLocation = true)
            locationService.getCurrentLocation()
                .onSuccess { location ->
                    _uiState.value = _uiState.value.copy(
                        isGettingLocation = false,
                        locationGranted = true,
                        userLatitude = location.latitude,
                        userLongitude = location.longitude
                    )
                }
                .onFailure {
                    locationService.getLastKnownLocation()
                        .onSuccess { location ->
                            _uiState.value = _uiState.value.copy(
                                isGettingLocation = false,
                                locationGranted = true,
                                userLatitude = location.latitude,
                                userLongitude = location.longitude
                            )
                        }
                        .onFailure {
                            _uiState.value = _uiState.value.copy(
                                isGettingLocation = false,
                                error = "تعذر تحديد الموقع، يرجى المحاولة مجدداً"
                            )
                        }
                }
        }
    }

    fun onLocationPermissionDenied() {
        _uiState.value = _uiState.value.copy(locationGranted = false, isGettingLocation = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearNoPlanFound() {
        _uiState.value = _uiState.value.copy(noPlanFound = false)
    }

    fun clearOrderCreationError() {
        _uiState.value = _uiState.value.copy(orderCreationError = null)
    }

    fun resetOrderCreated() {
        _uiState.value = _uiState.value.copy(orderCreated = false)
    }

    fun resetState(paymentType: String? = null) {
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
            error = null,
            noPlanFound = false,
            paymentType = paymentType
        )
        loadOngoingPlan()
    }

    fun updateMerchantSignName(signName: String, phoneNumber: String? = null) {
        val merchant = _uiState.value.selectedMerchant ?: return
        val lat = _uiState.value.userLatitude ?: return
        val lon = _uiState.value.userLongitude ?: return
        val formattedPhone = phoneNumber?.takeIf { it.isNotBlank() }?.let {
            if (it.startsWith("+")) it else "+2$it"
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingMerchantName = true, updateMerchantNameError = null)
            when (val result = updateMerchantUseCase(merchant.id, signName, lat, lon, formattedPhone)) {
                is ApiResult.Success -> {
                    val updated = merchant.copy(
                        sign_name = result.data.sign_name,
                        phone_number = result.data.phone_number ?: merchant.phone_number
                    )
                    _uiState.value = _uiState.value.copy(
                        isUpdatingMerchantName = false,
                        selectedMerchant = updated,
                        merchantSearchQuery = updated.displayName,
                        merchantNameUpdated = true
                    )
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isUpdatingMerchantName = false,
                    updateMerchantNameError = result.message
                )
                is ApiResult.Loading -> {}
            }
        }
    }

    fun clearMerchantNameUpdated() {
        _uiState.value = _uiState.value.copy(merchantNameUpdated = false, updateMerchantNameError = null)
    }

    private fun printInvoice(order: CreateOrderResponse) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isPrinting = true, printStatus = "جاري جلب الفاتورة...")

                val orderResult = getOrderByIdUseCase(order.id)
                if (orderResult !is ApiResult.Success) {
                    throw Exception("فشل جلب تفاصيل الطلب")
                }

                val attachment = orderResult.data.invoice_attachment
                    ?: throw Exception("لم يتم العثور على مرفق فاتورة")

                _uiState.value = _uiState.value.copy(printStatus = "جاري تحميل الفاتورة...")
                val invoiceResult = getInvoiceContentUseCase(attachment.url)
                if (invoiceResult !is ApiResult.Success) {
                    throw Exception("فشل تحميل الفاتورة")
                }

                _uiState.value = _uiState.value.copy(printStatus = "جاري تهيئة الفاتورة للطباعة...")
                val cpclBytes = CpclInvoiceFormatter.formatInvoiceAsCpclBytes(invoiceResult.data)

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
}
