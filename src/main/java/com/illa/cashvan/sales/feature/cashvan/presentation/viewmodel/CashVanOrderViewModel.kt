package com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.location.LocationService
import com.illa.cashvan.sales.domain.cashvan.CashvanCartItem
import com.illa.cashvan.sales.domain.cashvan.CashvanProduct
import com.illa.cashvan.sales.domain.cashvan.RecommendedProduct
import com.illa.cashvan.sales.domain.cashvan.RecommendedSectionCard
import com.illa.cashvan.sales.domain.cashvan.VisitRecommendationsResult
import com.illa.cashvan.sales.domain.cashvan.buildRecommendedSectionCards
import com.illa.cashvan.sales.domain.cashvan.excludeSectionProducts
import com.illa.cashvan.sales.domain.cashvan.findInvalidCashvanCartItems
import com.illa.cashvan.sales.domain.cashvan.validateRebateInput
import com.illa.cashvan.sales.domain.checkout.PaymentType
import com.illa.cashvan.sales.feature.cashvan.data.mapper.toCashVanProducts
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanMerchantContext
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanPlan
import com.illa.cashvan.sales.feature.cashvan.data.model.RailsMerchantPricing
import com.illa.cashvan.sales.feature.cashvan.data.model.RecommendedProductRow
import com.illa.cashvan.sales.feature.cashvan.data.model.VisitRecommendationsRow
import com.illa.cashvan.sales.feature.cashvan.domain.repository.CashVanOrderException
import com.illa.cashvan.sales.feature.cashvan.domain.repository.CashVanOrderRequest
import com.illa.cashvan.sales.feature.cashvan.domain.repository.CashVanRepository
import com.illa.cashvan.sales.feature.order.presentation.viewmodel.FawryInvoice
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt

data class CashVanOrderUiState(
    val merchant: CashVanMerchantContext? = null,
    val pricing: RailsMerchantPricing? = null,
    val plan: CashVanPlan? = null,
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val products: List<CashvanProduct> = emptyList(),
    val recommendations: VisitRecommendationsRow? = null,
    val search: String = "",
    val cart: List<CashvanCartItem> = emptyList(),
    val paymentType: PaymentType = PaymentType.CASH,
    val rebateInput: String = "",
    val fawryInvoice: FawryInvoice? = null,
    val placing: Boolean = false,
    val message: String? = null,
    /** Set once the order is created; the screen celebrates with this total. */
    val completedTotal: Double? = null
) {
    val recommendationsResult: VisitRecommendationsResult?
        get() = recommendations?.let { row ->
            VisitRecommendationsResult(
                predictedSpending = row.predicted_spending,
                recommendedProducts = row.recommended_products?.map { it.toDomain() },
                offTruckProducts = row.off_truck_products?.map { it.toDomain() }
            )
        }

    /** "موصى به لهذه الزيارة": hidden while searching. */
    val sectionCards: List<RecommendedSectionCard>
        get() = buildRecommendedSectionCards(products, recommendationsResult, search)

    val gridProducts: List<CashvanProduct>
        get() {
            val term = search.trim().lowercase()
            val matching = products.filter {
                term.isEmpty() || it.productName.lowercase().contains(term) || it.sku.lowercase().contains(term)
            }.sortedBy { it.cashVanAvailable <= 0 }
            return excludeSectionProducts(matching, sectionCards) { it.productId }
        }

    fun recommendedQuantity(productId: String): Int? = recommendations?.recommended_products
        ?.firstOrNull { it.product_id == productId }?.default_qty?.let { kotlin.math.floor(it).toInt() }

    fun cartQuantity(planProductId: String): Int = cart.firstOrNull { it.planProductId == planProductId }?.quantity ?: 0

    val cartUnits: Int get() = cart.sumOf { it.quantity }
    /** Σ unit price × quantity: before rebate and without exclusive VAT, as the spend gauge measures it. */
    val cartValue: Double get() = cart.sumOf { it.unitPrice * it.quantity }
    val subtotalWithVat: Double get() = cart.sumOf { it.priceDetails.finalPrice * it.quantity }
    val vatTotal: Double get() = cart.sumOf { it.priceDetails.vatAmount * it.quantity }
    val cashDiscountTotal: Double get() = cart.sumOf { it.priceDetails.cashDiscount * it.quantity }
    private val exclusiveVat: Double
        get() = cart.filter { it.priceDetails.vatIsExclusive }.sumOf { it.priceDetails.vatAmount * it.quantity }
    val totalBeforeRebate: Double get() = cartValue + exclusiveVat
    val rebate get() = validateRebateInput(rebateInput, totalBeforeRebate)
    val total: Double get() = totalBeforeRebate - (rebate.rebateValue.takeIf { rebate.errorMessage == null && it.isFinite() } ?: 0.0)

    /** Share of the predicted spend this cart reaches, capped at 100; null without a prediction. */
    val spendPercent: Int?
        get() = recommendations?.predicted_spending?.takeIf { it > 0 }?.let { minOf(100, (cartValue * 100 / it).roundToInt()) }
}

private fun RecommendedProductRow.toDomain() = RecommendedProduct(
    productId = product_id,
    sku = sku.orEmpty(),
    name = name.orEmpty(),
    score = score ?: 0.0,
    defaultQty = default_qty
)

/**
 * One cash van order, from product selection to checkout, as the PWA's `CashvanProductSelection` and
 * `CashvanCheckout` pages.
 */
class CashVanOrderViewModel(
    private val repository: CashVanRepository,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CashVanOrderUiState())
    val uiState: StateFlow<CashVanOrderUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    fun start(merchant: CashVanMerchantContext) {
        pollJob?.cancel()
        _uiState.value = CashVanOrderUiState(merchant = merchant, isLoading = true)
        viewModelScope.launch {
            try {
                val plan = repository.getOngoingPlan()
                if (plan?.id == null) {
                    _uiState.update { it.copy(isLoading = false, loadError = "لا يوجد خطة نشطة") }
                    return@launch
                }
                val pricing = repository.getMerchantPricing(merchant.merchantId)
                _uiState.update { it.copy(plan = plan, pricing = pricing) }
                loadProducts(plan.id, merchant.merchantId, pricing)
                _uiState.update { it.copy(recommendations = repository.getVisitRecommendations(merchant.merchantId, plan.id)) }
            } catch (e: Exception) {
                Timber.e(e, "Cash van order load failed")
                _uiState.update { it.copy(isLoading = false, loadError = "تعذر تحميل المنتجات، حاول مرة أخرى") }
                return@launch
            }
            pollPricing(merchant.merchantId)
        }
    }

    private suspend fun loadProducts(planId: String, merchantId: String, pricing: RailsMerchantPricing) {
        val tier = pricing.price_tier
        if (tier.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, loadError = "لا يوجد سعر مطابق لهذا العميل. تواصل مع الدعم.") }
            return
        }
        val products = repository.getPlanProducts(planId, merchantId).toCashVanProducts(pricing.governorate_id, tier)
        _uiState.update { state -> resyncCart(state.copy(products = products, isLoading = false, loadError = null)) }
    }

    /** The merchant's tier can change mid-visit; re-price every 5 s, as the PWA polls. */
    private fun pollPricing(merchantId: String) {
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(PRICING_POLL_MS)
                if (_uiState.value.placing) continue
                val latest = runCatching { repository.getMerchantPricing(merchantId) }.getOrNull() ?: continue
                val current = _uiState.value.pricing
                if (latest.price_tier != current?.price_tier || latest.governorate_id != current?.governorate_id) {
                    _uiState.update { it.copy(pricing = latest, message = "تم تحديث الأسعار — تم تغيير فئة السعر للعميل") }
                    val planId = _uiState.value.plan?.id ?: continue
                    runCatching { loadProducts(planId, merchantId, latest) }
                }
            }
        }
    }

    /** Re-prices cart lines from fresh products and drops ones that are no longer sellable. */
    private fun resyncCart(state: CashVanOrderUiState): CashVanOrderUiState {
        if (state.cart.isEmpty()) return state
        val invalid = findInvalidCashvanCartItems(state.cart, state.products).map { it.planProductId }.toSet()
        val byId = state.products.associateBy { it.planProductId }
        val cart = state.cart.filter { it.planProductId !in invalid }.mapNotNull { item ->
            val product = byId[item.planProductId] ?: return@mapNotNull null
            item.copy(
                unitPrice = product.price,
                priceDetails = product.priceDetails,
                maxQuantity = product.cashVanAvailable,
                quantity = minOf(item.quantity, product.cashVanAvailable)
            )
        }
        return state.copy(
            cart = cart,
            message = if (invalid.isNotEmpty()) "تم حذف منتجات غير متاحة أو بدون سعر مطابق. تواصل مع الدعم." else state.message
        )
    }

    fun onSearch(query: String) {
        _uiState.update { it.copy(search = query) }
    }

    fun add(product: CashvanProduct) {
        if (!product.hasValidPrice) {
            showMessage("لا يوجد سعر مطابق لهذا العميل. تواصل مع الدعم.")
            return
        }
        if (product.cashVanAvailable <= 0) return
        setQuantity(product, 1)
    }

    /** Zero removes the line; the truck's available stock caps it. */
    fun setQuantity(product: CashvanProduct, quantity: Int) {
        _uiState.update { state ->
            val capped = quantity.coerceIn(0, product.cashVanAvailable)
            val others = state.cart.filter { it.planProductId != product.planProductId }
            if (capped == 0) return@update state.copy(cart = others)
            val existing = state.cart.firstOrNull { it.planProductId == product.planProductId }
            val item = existing?.copy(quantity = capped) ?: CashvanCartItem(
                planProductId = product.planProductId,
                productId = product.productId,
                productName = product.productName,
                sku = product.sku,
                imageUrl = product.imageUrl,
                quantity = capped,
                maxQuantity = product.cashVanAvailable,
                unitPrice = product.price,
                priceDetails = product.priceDetails
            )
            state.copy(cart = if (existing != null) state.cart.map { if (it.planProductId == product.planProductId) item else it } else state.cart + item)
        }
    }

    fun remove(planProductId: String) {
        _uiState.update { state -> state.copy(cart = state.cart.filter { it.planProductId != planProductId }) }
    }

    fun setPaymentType(type: PaymentType) {
        _uiState.update { it.copy(paymentType = type, fawryInvoice = if (type == PaymentType.FAWRY) it.fawryInvoice else null) }
    }

    fun setRebate(input: String) {
        _uiState.update { it.copy(rebateInput = input) }
    }

    fun setFawryInvoice(invoice: FawryInvoice?) {
        _uiState.update { it.copy(fawryInvoice = invoice) }
    }

    fun placeOrder() {
        val state = _uiState.value
        val plan = state.plan ?: return
        val merchant = state.merchant ?: return
        if (state.placing || state.cart.isEmpty()) return
        state.rebate.errorMessage?.let {
            showMessage(it)
            return
        }
        if (state.paymentType == PaymentType.FAWRY && state.fawryInvoice == null) {
            showMessage("يرجى رفع صورة الفاتورة أولاً")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(placing = true) }
            try {
                // The location only travels as order metadata; there is no distance check at checkout.
                val location = locationService.getCurrentLocation().getOrNull()
                if (location == null) {
                    showMessage("تعذر تحديد موقعك، يرجى المحاولة مرة أخرى")
                    return@launch
                }
                repository.createOrder(
                    CashVanOrderRequest(
                        planId = plan.id ?: return@launch,
                        merchantId = merchant.merchantId,
                        paymentType = state.paymentType.wireValue,
                        rebateValue = state.rebate.rebateValue.takeIf { it.isFinite() } ?: 0.0,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        items = state.cart.associate { it.planProductId to it.quantity },
                        fawryInvoiceDataUrl = state.fawryInvoice?.dataUrl
                    )
                )
                pollJob?.cancel()
                _uiState.update { it.copy(completedTotal = state.total, cart = emptyList()) }
            } catch (e: CashVanOrderException) {
                showMessage(e.message ?: "فشل إنشاء الطلب، يرجى المحاولة مرة أخرى")
            } finally {
                _uiState.update { it.copy(placing = false) }
            }
        }
    }

    /** Leaving the order flow drops the cart and stops polling, as the PWA's back does. */
    fun discard() {
        pollJob?.cancel()
        _uiState.value = CashVanOrderUiState()
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    private companion object {
        const val PRICING_POLL_MS = 5_000L
    }
}
