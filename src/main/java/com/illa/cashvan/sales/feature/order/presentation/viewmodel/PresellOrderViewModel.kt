package com.illa.cashvan.sales.feature.order.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.analytics.SalesAnalytics
import com.illa.cashvan.core.location.LocationData
import com.illa.cashvan.core.location.LocationService
import com.illa.cashvan.sales.data.CurrentAgentProvider
import com.illa.cashvan.sales.domain.cart.PriceTier
import com.illa.cashvan.sales.domain.cart.applyFreshPrices
import com.illa.cashvan.sales.domain.checkout.CheckoutConfirmBlockReason
import com.illa.cashvan.sales.domain.checkout.CheckoutConfirmGuard
import com.illa.cashvan.sales.domain.checkout.CheckoutConfirmGuardInput
import com.illa.cashvan.sales.domain.checkout.DeferredRequestOutcome
import com.illa.cashvan.sales.domain.checkout.OrderSubmissionFailure
import com.illa.cashvan.sales.domain.checkout.PaymentType
import com.illa.cashvan.sales.domain.checkout.PostSubmissionOutcomeInput
import com.illa.cashvan.sales.domain.checkout.PricingDriftInput
import com.illa.cashvan.sales.domain.checkout.ShortageLine
import com.illa.cashvan.sales.domain.checkout.resolveConfirmGuard
import com.illa.cashvan.sales.domain.checkout.resolveOrderSubmitFeedback
import com.illa.cashvan.sales.domain.checkout.resolvePostSubmissionOutcome
import com.illa.cashvan.sales.domain.checkout.resolvePricingDrift
import com.illa.cashvan.sales.domain.inventory.InventoryCartLine
import com.illa.cashvan.sales.domain.inventory.ReservationSource
import com.illa.cashvan.sales.domain.inventory.assessPresellCartInventory
import com.illa.cashvan.sales.domain.inventory.cartContainsExpectedReservations
import com.illa.cashvan.sales.domain.inventory.collectPresellRequestItemsInStock
import com.illa.cashvan.sales.domain.inventory.getPresellInventoryContext
import com.illa.cashvan.sales.domain.inventory.PresellInventoryReadState
import com.illa.cashvan.sales.domain.model.PresellOrderContext
import com.illa.cashvan.sales.domain.outofstock.OutOfStockRequestItemInput
import com.illa.cashvan.sales.domain.outofstock.SubmitOutOfStockRequestInput
import com.illa.cashvan.sales.domain.outofstock.SubmitOutOfStockRequestResult
import com.illa.cashvan.sales.feature.cart.data.model.StoredProduct
import com.illa.cashvan.sales.feature.cart.domain.repository.PresellCartRepository
import com.illa.cashvan.sales.feature.catalog.data.mapper.PresellInventorySnapshot
import com.illa.cashvan.sales.feature.catalog.data.mapper.toCartItem
import com.illa.cashvan.sales.feature.catalog.data.mapper.toInventorySnapshot
import com.illa.cashvan.sales.feature.catalog.data.mapper.toPriceInfo
import com.illa.cashvan.sales.feature.catalog.data.mapper.toStoredProduct
import com.illa.cashvan.sales.feature.catalog.data.model.MerchantPricing
import com.illa.cashvan.sales.feature.catalog.domain.repository.CatalogRepository
import com.illa.cashvan.sales.feature.checkout.data.model.OrderLine
import com.illa.cashvan.sales.feature.checkout.data.model.PlaceOrderRequest
import com.illa.cashvan.sales.feature.checkout.domain.repository.OrderSubmissionException
import com.illa.cashvan.sales.feature.checkout.domain.repository.PresellCheckoutRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * One presell order, from vendor selection to checkout. Shared by the vendor, product and checkout screens,
 * like the PWA's cart and out-of-stock request contexts plus its presell flow context.
 */
class PresellOrderViewModel(
    private val catalogRepository: CatalogRepository,
    private val cartRepository: PresellCartRepository,
    private val checkoutRepository: PresellCheckoutRepository,
    private val currentAgentProvider: CurrentAgentProvider,
    private val locationService: LocationService,
    private val analytics: SalesAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(PresellOrderUiState())
    val uiState: StateFlow<PresellOrderUiState> = _uiState.asStateFlow()

    private var basketJobs: List<Job> = emptyList()
    private var productsJob: Job? = null

    /** Starts a new order for the merchant in [context]; the merchant's saved cart, if any, comes back. */
    fun start(context: PresellOrderContext) {
        basketJobs.forEach { it.cancel() }
        productsJob?.cancel()
        _uiState.value = PresellOrderUiState(context = context)
        basketJobs = listOf(
            viewModelScope.launch {
                cartRepository.observeCart(context.merchantId).collect { lines -> _uiState.update { it.copy(cart = lines) } }
            },
            viewModelScope.launch {
                cartRepository.observeRequests(context.merchantId).collect { lines -> _uiState.update { it.copy(requests = lines) } }
            }
        )
        loadVendors()
    }

    fun loadVendors() {
        val context = _uiState.value.context ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(vendorsLoading = true, loadFailed = false, noRegion = false) }
            try {
                // The merchant's tier and region decide every price; navigation state is only a fallback.
                val pricing = catalogRepository.getMerchantPricing(context.merchantId)
                    ?: MerchantPricing(context.priceTier, context.regionId)
                if (pricing.regionId == null) {
                    _uiState.update { it.copy(pricing = pricing, vendorsLoading = false, noRegion = true, vendors = emptyList()) }
                    return@launch
                }
                val catalog = catalogRepository.getVendors(pricing)
                _uiState.update {
                    it.copy(pricing = pricing, vendors = catalog.vendors, vendorSkus = catalog.vendorSkus, vendorsLoading = false)
                }
                refreshInventory(manual = false)
            } catch (e: Exception) {
                Timber.e(e, "Vendor load failed")
                _uiState.update { it.copy(vendorsLoading = false, loadFailed = true, message = OrderMessage("خطأ", "فشل تحميل المنتجات")) }
            }
        }
    }

    fun openVendor(principalId: String?) {
        val pricing = _uiState.value.pricing ?: return
        productsJob?.cancel()
        _uiState.update { it.copy(selectedVendorId = principalId, products = emptyList(), productsLoading = true, productSearch = "") }
        productsJob = viewModelScope.launch {
            try {
                val products = catalogRepository.getProducts(pricing, principalId).map { it.toStoredProduct() }
                _uiState.update { it.copy(products = products, productsLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Product load failed")
                _uiState.update { it.copy(productsLoading = false, message = OrderMessage("خطأ", "فشل تحميل المنتجات")) }
            }
        }
    }

    fun onProductSearch(query: String) {
        _uiState.update { it.copy(productSearch = query) }
    }

    /** Re-reads balances for every SKU the merchant can be sold, plus anything already in a basket. */
    fun refreshInventory(manual: Boolean = true) {
        viewModelScope.launch { loadInventory(manual) }
    }

    private suspend fun loadInventory(manual: Boolean): PresellInventorySnapshot? {
        val state = _uiState.value
        val context = state.context ?: return null
        val skus = (state.vendorSkus.map { it.second } + state.cart.map { it.product.sku } + state.requests.map { it.product.sku }).distinct()
        _uiState.update { if (manual) it.copy(inventoryRefreshing = true) else it.copy(inventoryLoading = true) }
        return try {
            val snapshot = catalogRepository.getPresellInventory(context.merchantId, skus).toInventorySnapshot()
            _uiState.update { it.copy(inventory = snapshot, inventoryLoading = false, inventoryRefreshing = false, inventoryError = null) }
            snapshot
        } catch (e: Exception) {
            Timber.e(e, "Inventory load failed")
            _uiState.update { it.copy(inventoryLoading = false, inventoryRefreshing = false, inventoryError = e.message ?: "inventory error") }
            null
        }
    }

    // region Cart

    fun addToCart(product: StoredProduct) {
        val state = _uiState.value
        val policy = state.policyFor(product)
        if (!policy.canAdd) {
            showMessage(OrderMessage("لا يمكن إضافة المنتج", policy.label))
            return
        }
        if (state.requests.isNotEmpty() && policy.reservationSource == ReservationSource.EXPECTED) {
            showMessage(OrderMessage("لا يمكن إضافة منتج من المخزون المتوقع", EXPECTED_WITH_REQUESTS))
            return
        }
        val principalName = state.vendors.firstOrNull { it.id == product.principal_id }?.name ?: "مورد غير معروف"
        analytics.track(
            "cart_item_added",
            "merchant_id" to state.context?.merchantId,
            "product_id" to product.id,
            "product_name" to product.description,
            "sku" to product.sku,
            "quantity" to 1,
            "unit_price" to product.price,
            "inventory_availability" to policy.reservationSource.name.lowercase(),
            "available_quantity" to policy.availableUnits
        )
        viewModelScope.launch { withMerchant { cartRepository.addToCart(it, product, 1, principalName) } }
    }

    fun setCartQuantity(product: StoredProduct, quantity: Int) {
        val state = _uiState.value
        if (quantity > state.cartQuantity(product.id)) {
            val policy = state.policyFor(product)
            if (state.requests.isNotEmpty() && policy.reservationSource == ReservationSource.EXPECTED) {
                showMessage(OrderMessage("لا يمكن زيادة منتج من المخزون المتوقع", EXPECTED_WITH_REQUESTS))
                return
            }
        }
        if (quantity <= 0) {
            analytics.track("cart_item_removed", "merchant_id" to state.context?.merchantId, "product_id" to product.id)
        } else {
            analytics.track("cart_item_quantity_changed", "merchant_id" to state.context?.merchantId, "product_id" to product.id, "new_quantity" to quantity)
        }
        viewModelScope.launch { withMerchant { cartRepository.setCartQuantity(it, product.id, quantity) } }
    }

    /** DIS-446: an out-of-stock product goes to the deferred request basket instead of the cart. */
    fun setRequestQuantity(product: StoredProduct, quantity: Int) {
        val state = _uiState.value
        if (quantity > 0 && cartContainsExpectedReservations(state.cartInventoryLines, state.stocks, state.inventoryContext)) {
            showMessage(OrderMessage("لا يمكن إضافة طلب مؤجل", EXPECTED_WITH_REQUESTS_FROM_REQUEST))
            return
        }
        val previous = state.requestQuantity(product.id)
        viewModelScope.launch {
            withMerchant { merchantId ->
                cartRepository.setRequestQuantity(merchantId, product, quantity)
                val requestId = cartRepository.requestId(merchantId)
                when {
                    previous == 0 && quantity > 0 -> analytics.track(
                        "deferred_request_item_added",
                        "merchant_id" to merchantId, "request_id" to requestId, "product_id" to product.id,
                        "product_name" to product.description, "sku" to product.sku, "quantity" to quantity,
                        "expected_receipt_date" to state.policyFor(product).expectedReceiptDate,
                        "origin" to "product_page", "source" to SOURCE
                    )
                    previous > 0 && quantity <= 0 -> analytics.track(
                        "deferred_request_item_removed",
                        "merchant_id" to merchantId, "request_id" to requestId, "product_id" to product.id,
                        "product_name" to product.description, "sku" to product.sku, "requested_quantity" to previous,
                        "source" to SOURCE
                    )
                }
            }
        }
    }

    /** Fired once each time the review page opens with something in a basket. */
    fun onCheckoutOpened() {
        val state = _uiState.value
        if (!state.lanes.hasItems) return
        analytics.track(
            "checkout_sheet_opened",
            "merchant_id" to state.context?.merchantId,
            "merchant_name" to state.context?.merchantName,
            "items_count" to state.cart.size,
            "total_value" to state.cartTotal,
            "lane" to laneName(state),
            "request_items_count" to state.requests.size,
            "request_total_quantity" to state.requests.sumOf { it.quantity }
        )
    }

    private fun laneName(state: PresellOrderUiState) = when {
        state.lanes.hasCartItems && state.lanes.hasDeferredItems -> "combined"
        state.lanes.hasDeferredItems -> "request-only"
        else -> "order-only"
    }

    /** Moves a shortaged cart line to the deferred basket (checkout review). */
    fun convertToRequest(product: StoredProduct) {
        val state = _uiState.value
        val remaining = state.cart.filter { it.product.id != product.id }
            .map { InventoryCartLine(it.product.sku, it.product.description, it.quantity) }
        if (cartContainsExpectedReservations(remaining, state.stocks, state.inventoryContext)) {
            showMessage(OrderMessage("لا يمكن تحويل هذا المنتج إلى طلب مؤجل", EXPECTED_WITH_REQUESTS_FROM_REQUEST))
            return
        }
        val quantity = state.cartQuantity(product.id)
        viewModelScope.launch {
            withMerchant {
                cartRepository.setCartQuantity(it, product.id, 0)
                cartRepository.setRequestQuantity(it, product, maxOf(quantity, 1))
            }
            showMessage(OrderMessage("تم تحويل المنتج إلى طلب مؤجل", isError = false))
        }
    }

    // endregion

    // region Checkout

    fun setPaymentType(type: PaymentType) {
        _uiState.update {
            it.copy(
                paymentType = type,
                collectionDate = if (type == PaymentType.CREDIT) it.collectionDate else null,
                fawryInvoice = if (type == PaymentType.FAWRY) it.fawryInvoice else null
            )
        }
    }

    fun setCollectionDate(date: LocalDate?) {
        _uiState.update { it.copy(collectionDate = date) }
    }

    fun setFawryInvoice(invoice: FawryInvoice?) {
        _uiState.update { it.copy(fawryInvoice = invoice) }
    }

    /**
     * Port of the PWA's `handleCheckoutConfirm`: guard → fresh inventory check → Fawry upload → order →
     * deferred request → outcome.
     */
    fun confirmCheckout() {
        val context = _uiState.value.context ?: return
        if (_uiState.value.placing) return
        viewModelScope.launch {
            _uiState.update { it.copy(placing = true) }
            try {
                // A fresh fix, falling back to the geofence-approved one; the order RPC records it.
                val location = locationService.getCurrentLocation().getOrNull() ?: context.visitLocation
                val state = _uiState.value
                val guard = resolveConfirmGuard(
                    CheckoutConfirmGuardInput(
                        lanes = state.lanes,
                        conflictDetected = state.conflictDetected,
                        locationReady = location != null,
                        paymentType = state.paymentType,
                        hasFawryInvoice = state.fawryInvoice != null,
                        cartBlocked = state.cartAssessment.blocked,
                        requestBlocked = state.lanes.hasDeferredItems && state.requestBlocked
                    )
                )
                if (guard is CheckoutConfirmGuard.Blocked) {
                    showMessage(guardMessage(guard.reason, state))
                    return@launch
                }
                runCheckout(context, location)
            } finally {
                _uiState.update { it.copy(placing = false) }
            }
        }
    }

    private suspend fun runCheckout(context: PresellOrderContext, location: LocationData?) {
        val lanes = _uiState.value.lanes
        if (lanes.hasCartItems || lanes.hasDeferredItems) {
            val fresh = loadInventory(manual = true) ?: return
            val freshContext = getPresellInventoryContext(
                PresellInventoryReadState(fresh.config, fresh.fetchedAt, fresh.isStale, loading = false, refreshing = false, error = null)
            )
            if (freshContext.enforcementEnabled == true) {
                val state = _uiState.value
                if (lanes.hasCartItems && assessPresellCartInventory(state.cartInventoryLines, fresh.stocks, freshContext).blocked) {
                    showMessage(OrderMessage("تم تحديث حالة المخزون", state.cartAssessment.systemMessage ?: "عدّل السلة قبل تأكيد الطلب."))
                    return
                }
                if (lanes.hasDeferredItems) {
                    val flips = collectPresellRequestItemsInStock(state.requests, fresh.stocks, freshContext) { it.product.sku }
                    if (freshContext.orderBlocked || !freshContext.controlsReady || flips.isNotEmpty()) {
                        showMessage(
                            OrderMessage(
                                "تم تحديث حالة المخزون",
                                if (flips.isNotEmpty()) "منتجات مؤجلة أصبحت متاحة الآن — أضفها إلى السلة أو احذفها ثم أعد تأكيد الطلب."
                                else freshContext.blockMessage ?: "راجع حالة المخزون ثم أعد المحاولة."
                            )
                        )
                        return
                    }
                }
            }
        }

        var fawryInvoiceUrl: String? = null
        val invoice = _uiState.value.fawryInvoice
        if (lanes.hasCartItems && _uiState.value.paymentType == PaymentType.FAWRY && invoice != null) {
            fawryInvoiceUrl = try {
                checkoutRepository.uploadFawryInvoice(invoice.dataUrl, invoice.contentType)
            } catch (e: Exception) {
                Timber.e(e, "Fawry upload failed")
                showMessage(OrderMessage("خطأ", "فشل رفع ملف الفاتورة"))
                return
            }
        }

        var orderPlaced = false
        if (lanes.hasCartItems) {
            if (!placeOrder(context, location, fawryInvoiceUrl)) return
            cartRepository.clearCart(context.merchantId)
            orderPlaced = true
        }

        var deferredOutcome = DeferredRequestOutcome.NOT_ATTEMPTED
        if (lanes.hasDeferredItems) {
            deferredOutcome = submitDeferredRequest(context, lanes.hasCartItems)
        }

        val outcome = resolvePostSubmissionOutcome(PostSubmissionOutcomeInput(lanes, orderPlaced, deferredOutcome))
        when {
            outcome.showCelebration -> finish(OrderCompletion.CELEBRATE)
            outcome.showRequestDialog -> finish(OrderCompletion.REQUEST_SUBMITTED)
        }
    }

    /** Port of `handlePlaceOrder`, including the DIS-446 pricing drift guard. */
    private suspend fun placeOrder(context: PresellOrderContext, location: LocationData?, fawryInvoiceUrl: String?): Boolean {
        val state = _uiState.value
        if (state.cartAssessment.blocked) {
            showMessage(OrderMessage("راجع رصيد المنتجات", state.cartAssessment.systemMessage ?: "توجد منتجات بكميات أعلى من المتاح. عدّل السلة قبل تأكيد الطلب."))
            return false
        }

        var pricing = state.pricing ?: MerchantPricing(context.priceTier, context.regionId)
        catalogRepository.getMerchantPricing(context.merchantId)?.let { fetched ->
            val drift = resolvePricingDrift(
                PricingDriftInput(
                    liveTier = PriceTier.fromWire(pricing.priceTier) ?: PriceTier.RETAIL,
                    liveRegionId = pricing.regionId,
                    fetchedTier = PriceTier.fromWire(fetched.priceTier),
                    fetchedRegionId = fetched.regionId
                )
            )
            pricing = MerchantPricing(drift.tier.wireValue, drift.regionId)
            if (drift.drifted) {
                _uiState.update { it.copy(pricing = pricing) }
                val unfixable = refreshCartPrices(context.merchantId, pricing)
                showMessage(
                    OrderMessage(
                        "تم تحديث بيانات العميل",
                        if (unfixable.isNotEmpty()) "تم تغيير فئة السعر أو المحافظة. راجع المنتجات بلا سعر: ${unfixable.joinToString("، ")}"
                        else "تم تغيير فئة السعر أو المحافظة، وتم تحديث الأسعار. راجع الإجمالي الجديد وأعد تأكيد الطلب."
                    )
                )
                return false
            }
        }

        val unfixable = refreshCartPrices(context.merchantId, pricing)
        if (unfixable.isNotEmpty()) {
            showMessage(OrderMessage("خطأ", "منتجات بدون سعر: ${unfixable.joinToString("، ")}. احذفها من السلة"))
            return false
        }

        val cart = _uiState.value.cart
        val totalValue = cart.sumOf { it.product.price * it.quantity }
        return try {
            checkoutRepository.placeOrder(
                PlaceOrderRequest(
                    merchantId = context.merchantId,
                    priceTier = pricing.priceTier,
                    paymentType = _uiState.value.paymentType.wireValue,
                    collectionDate = _uiState.value.collectionDate?.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    isAdHoc = context.isAdHoc,
                    location = location,
                    fawryInvoiceUrl = fawryInvoiceUrl,
                    totalValue = cart.sumOf { it.product.price * it.quantity },
                    idempotencyKey = cartRepository.idempotencyKey(context.merchantId),
                    regionId = pricing.regionId,
                    lines = cart.map { OrderLine(it.product.id, it.quantity, it.product.price, it.product.price_info) }
                )
            )
            analytics.track(
                "order_placed",
                "merchant_id" to context.merchantId,
                "merchant_name" to context.merchantName,
                "payment_type" to _uiState.value.paymentType.wireValue,
                "collection_date" to _uiState.value.collectionDate?.format(DateTimeFormatter.ISO_LOCAL_DATE),
                "products" to cart.map {
                    mapOf("id" to it.product.id, "description" to it.product.description, "quantity" to it.quantity, "unit_price" to it.product.price)
                },
                "total_value" to totalValue,
                "total_items" to cart.sumOf { it.quantity },
                "product_count" to cart.size
            )
            true
        } catch (e: Exception) {
            Timber.e(e, "Place order failed")
            analytics.track("order_submission_failed", "merchant_id" to context.merchantId, "error_message" to (e.message ?: e::class.simpleName), "total_value" to totalValue)
            val failure = (e as? OrderSubmissionException)?.let { error ->
                OrderSubmissionFailure(
                    code = error.code,
                    shortages = error.shortages.map {
                        ShortageLine(it.product_id, it.sku, it.requested.toInt(), it.available.toInt(), it.status, it.actual_available?.toInt())
                    },
                    reason = error.reason
                )
            }
            val feedback = resolveOrderSubmitFeedback(failure)
            showMessage(OrderMessage(feedback.title, feedback.description))
            if (feedback.refreshInventory) refreshInventory()
            false
        }
    }

    /**
     * Re-prices every cart line for [pricing]. Returns the descriptions of lines with no price there; the
     * agent has to remove them.
     */
    private suspend fun refreshCartPrices(merchantId: String, pricing: MerchantPricing): List<String> {
        val cart = _uiState.value.cart
        if (cart.isEmpty()) return emptyList()
        val rows = try {
            catalogRepository.getPriceRows(cart.map { it.product.id }, pricing)
        } catch (e: Exception) {
            Timber.e(e, "Price refresh failed")
            return cart.map { it.product.description }
        }
        if (pricing.regionId == null) return cart.map { it.product.description }

        val result = applyFreshPrices(cart.map { it.toCartItem() }, rows.map { it.toPriceInfo() })
        val rowByProduct = rows.associateBy { (it["product_id"] as? JsonPrimitive)?.contentOrNull }
        val updated = result.updatedItems.mapNotNull { item ->
            val stored = cart.firstOrNull { it.product.id == item.productId }?.product ?: return@mapNotNull null
            stored.copy(price = item.product.price, price_info = rowByProduct[item.productId] ?: stored.price_info)
        }
        cartRepository.updateProducts(merchantId, updated)
        return result.unfixableDescriptions
    }

    private suspend fun submitDeferredRequest(context: PresellOrderContext, hasCartItems: Boolean): DeferredRequestOutcome {
        val requests = _uiState.value.requests
        val requestId = cartRepository.requestId(context.merchantId)
        val lane = if (hasCartItems) "combined" else "request-only"
        val result = checkoutRepository.submitOutOfStockRequest(
            SubmitOutOfStockRequestInput(
                requestId = requestId,
                agentId = currentAgentProvider.require().agentId,
                merchantId = context.merchantId,
                items = requests.map { OutOfStockRequestItemInput(it.product.id, it.quantity) }
            )
        )
        val products = requests.map {
            mapOf("product_id" to it.product.id, "product_name" to it.product.description, "sku" to it.product.sku, "quantity" to it.quantity)
        }
        when (result) {
            is SubmitOutOfStockRequestResult.Failed, SubmitOutOfStockRequestResult.RoutingFailed -> analytics.track(
                "deferred_request_submit_failed",
                "merchant_id" to context.merchantId, "request_id" to requestId, "lane" to lane,
                "error_type" to if (result == SubmitOutOfStockRequestResult.RoutingFailed) "routing_failed" else "failed",
                "error_message" to (result as? SubmitOutOfStockRequestResult.Failed)?.message,
                "request_items_count" to requests.size, "order_placed" to hasCartItems, "source" to SOURCE
            )
            else -> analytics.track(
                "deferred_request_submit_succeeded",
                "merchant_id" to context.merchantId, "request_id" to requestId, "lane" to lane,
                "result" to when (result) {
                    SubmitOutOfStockRequestResult.IdempotentReplay -> "idempotent_replay"
                    SubmitOutOfStockRequestResult.TerminalStatus -> "terminal_status"
                    else -> "submitted"
                },
                "request_items_count" to requests.size, "request_total_quantity" to requests.sumOf { it.quantity },
                "products" to products, "source" to SOURCE
            )
        }
        return when (result) {
            is SubmitOutOfStockRequestResult.Failed, SubmitOutOfStockRequestResult.RoutingFailed -> {
                showMessage(
                    OrderMessage(
                        if (hasCartItems) "تم إنشاء الطلب، وتعذر تسجيل الطلبات المؤجلة" else "تعذر تسجيل الطلب المؤجل",
                        if (result == SubmitOutOfStockRequestResult.RoutingFailed) {
                            "لم يتم ربط محافظة العميل بمخزن نشط. لا يمكن تسجيل الطلب المؤجل قبل ضبط ربط المخزن."
                        } else {
                            "حدث خطأ أثناء تسجيل الطلبات المؤجلة. يمكنك إعادة المحاولة من نفس الشاشة."
                        }
                    )
                )
                if (result == SubmitOutOfStockRequestResult.RoutingFailed) DeferredRequestOutcome.ROUTING_FAILED else DeferredRequestOutcome.FAILED
            }
            SubmitOutOfStockRequestResult.TerminalStatus -> {
                cartRepository.clearRequests(context.merchantId)
                showMessage(
                    OrderMessage(
                        if (hasCartItems) "تم إنشاء الطلب، والطلب المؤجل تمت معالجته مسبقًا" else "الطلب المؤجل تمت معالجته مسبقًا",
                        "قام فريق الطلبات بمعالجة هذا الطلب بالفعل، فلا يمكن تسجيله مرة أخرى.",
                        isError = false
                    )
                )
                DeferredRequestOutcome.TERMINAL_STATUS
            }
            SubmitOutOfStockRequestResult.Submitted, SubmitOutOfStockRequestResult.IdempotentReplay -> {
                cartRepository.clearRequests(context.merchantId)
                if (hasCartItems) {
                    showMessage(OrderMessage("تم تسجيل الطلبات المؤجلة", "تم تسجيل ${requests.size} منتج للمتابعة عند توفر المخزون.", isError = false))
                }
                if (result == SubmitOutOfStockRequestResult.Submitted) DeferredRequestOutcome.SUBMITTED else DeferredRequestOutcome.IDEMPOTENT_REPLAY
            }
        }
    }

    /** The screen shows the outcome, then returns to the route. */
    private fun finish(completion: OrderCompletion) {
        _uiState.update { it.copy(completion = completion) }
    }

    // endregion

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }

    /** Leaving the flow without ordering empties both baskets, as the PWA's exit does. */
    fun discardOrder() {
        val merchantId = _uiState.value.context?.merchantId ?: return
        analytics.track("cart_cleared", "merchant_id" to merchantId, "items_count_before_clear" to _uiState.value.cart.size)
        viewModelScope.launch {
            cartRepository.clearCart(merchantId)
            cartRepository.clearRequests(merchantId)
        }
    }

    private fun guardMessage(reason: CheckoutConfirmBlockReason, state: PresellOrderUiState): OrderMessage = when (reason) {
        CheckoutConfirmBlockReason.CONFLICT -> OrderMessage(
            "لا يمكن الجمع بين الطلبات المؤجلة والمنتجات المحجوزة",
            "الطلبات المؤجلة تُسجل مع المنتجات المتاحة فقط أو في طلب منفصل. احذف أحد النوعين للمتابعة."
        )
        CheckoutConfirmBlockReason.CART_BLOCKED -> OrderMessage("تم تحديث حالة المخزون", state.cartAssessment.systemMessage ?: "عدّل السلة قبل تأكيد الطلب.")
        CheckoutConfirmBlockReason.REQUEST_BLOCKED -> OrderMessage(
            "لا يمكن تسجيل الطلب المؤجل",
            state.inventoryContext.blockMessage ?: "تعذر التحقق من رصيد المخزن أو ربط محافظة العميل بمخزن نشط."
        )
        CheckoutConfirmBlockReason.FAWRY_INVOICE -> OrderMessage("خطأ", "ارفع صورة فاتورة فوري قبل تأكيد الطلب")
        CheckoutConfirmBlockReason.LOCATION -> OrderMessage("خطأ في تحديد الموقع", "مقدرناش نحدد مكانك. ظبط الـ GPS وحاول تاني.")
        CheckoutConfirmBlockReason.EMPTY -> OrderMessage("السلة فاضية", "أضف منتجات قبل تأكيد الطلب")
    }

    private suspend fun withMerchant(block: suspend (String) -> Unit) {
        val merchantId = _uiState.value.context?.merchantId ?: return
        block(merchantId)
    }

    private fun showMessage(message: OrderMessage) {
        _uiState.update { it.copy(message = message) }
    }

    private companion object {
        const val SOURCE = "android_native"
        const val EXPECTED_WITH_REQUESTS =
            "لا يمكن جمع المنتجات المحجوزة من المخزون المتوقع مع الطلبات المؤجلة. احذف الطلبات المؤجلة أو أنشئ طلبًا منفصلًا."
        const val EXPECTED_WITH_REQUESTS_FROM_REQUEST =
            "لا يمكن جمع الطلبات المؤجلة مع المنتجات المحجوزة من المخزون المتوقع. احذف المنتجات المحجوزة أو أنشئ طلبًا منفصلًا."
    }
}
