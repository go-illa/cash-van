package com.illa.cashvan.sales.feature.order.presentation.viewmodel

import com.illa.cashvan.sales.domain.checkout.CheckoutReviewLanes
import com.illa.cashvan.sales.domain.checkout.PaymentType
import com.illa.cashvan.sales.domain.checkout.resolveCheckoutLanes
import com.illa.cashvan.sales.domain.inventory.InventoryCartItemLike
import com.illa.cashvan.sales.domain.inventory.InventoryCartLine
import com.illa.cashvan.sales.domain.inventory.PresellCartInventoryAssessment
import com.illa.cashvan.sales.domain.inventory.PresellInventoryContext
import com.illa.cashvan.sales.domain.inventory.PresellInventoryReadState
import com.illa.cashvan.sales.domain.inventory.PresellInventoryRoutingStatus
import com.illa.cashvan.sales.domain.inventory.PresellProductInventoryPolicy
import com.illa.cashvan.sales.domain.inventory.PresellStockBalance
import com.illa.cashvan.sales.domain.inventory.assessPresellCartInventory
import com.illa.cashvan.sales.domain.inventory.cartContainsExpectedReservations
import com.illa.cashvan.sales.domain.inventory.getPresellInventoryContext
import com.illa.cashvan.sales.domain.inventory.getPresellProductInventoryPolicy
import com.illa.cashvan.sales.domain.inventory.getPresellStockBalance
import com.illa.cashvan.sales.domain.inventory.sortPresellProductsByAvailability
import com.illa.cashvan.sales.domain.model.PresellOrderContext
import com.illa.cashvan.sales.feature.cart.data.model.StoredCartLine
import com.illa.cashvan.sales.feature.cart.data.model.StoredProduct
import com.illa.cashvan.sales.feature.cart.data.model.StoredRequestLine
import com.illa.cashvan.sales.feature.catalog.data.mapper.PresellInventorySnapshot
import com.illa.cashvan.sales.feature.catalog.data.model.MerchantPricing
import com.illa.cashvan.sales.feature.catalog.data.model.Vendor
import java.time.LocalDate

data class FawryInvoice(
    /** `data:<type>;base64,...`, as the upload function expects. */
    val dataUrl: String,
    val contentType: String,
    val fileName: String
)

data class OrderMessage(val title: String, val description: String? = null, val isError: Boolean = true)

enum class OrderCompletion {
    /** Order (and any deferred request) recorded: celebrate, then go back to the route. */
    CELEBRATE,
    /** Only a deferred request was recorded: confirm it, then go back to the route. */
    REQUEST_SUBMITTED
}

data class PresellOrderUiState(
    val context: PresellOrderContext? = null,
    val pricing: MerchantPricing? = null,
    val vendorsLoading: Boolean = false,
    val vendors: List<Vendor> = emptyList(),
    val vendorSkus: List<Pair<String, String>> = emptyList(),
    val noRegion: Boolean = false,
    val loadFailed: Boolean = false,
    val selectedVendorId: String? = null,
    val productsLoading: Boolean = false,
    val products: List<StoredProduct> = emptyList(),
    val productSearch: String = "",
    val inventory: PresellInventorySnapshot? = null,
    val inventoryLoading: Boolean = false,
    val inventoryRefreshing: Boolean = false,
    val inventoryError: String? = null,
    val cart: List<StoredCartLine> = emptyList(),
    val requests: List<StoredRequestLine> = emptyList(),
    val paymentType: PaymentType = PaymentType.CASH,
    val collectionDate: LocalDate? = null,
    val fawryInvoice: FawryInvoice? = null,
    val placing: Boolean = false,
    val message: OrderMessage? = null,
    val completion: OrderCompletion? = null
) {
    val stocks: Map<String, PresellStockBalance> get() = inventory?.stocks.orEmpty()

    val inventoryContext: PresellInventoryContext
        get() = getPresellInventoryContext(
            PresellInventoryReadState(
                config = inventory?.config,
                fetchedAt = inventory?.fetchedAt,
                isStale = inventory?.isStale ?: true,
                loading = inventoryLoading,
                refreshing = inventoryRefreshing,
                error = inventoryError
            )
        )

    /** DIS-446: once balances are in, products without one are warehouse not-found SKUs. */
    val inventoryReady: Boolean
        get() = !inventoryLoading && inventoryError == null && inventory?.fetchedAt != null &&
            inventory.config?.routingStatus == PresellInventoryRoutingStatus.CONFIGURED

    /** Vendors with at least one stocked product once inventory is ready; all vendors until then. */
    val visibleVendors: List<Vendor>
        get() {
            if (!inventoryReady) return vendors
            val stocked = vendorSkus.filter { (_, sku) -> getPresellStockBalance(stocks, sku) != null }
                .groupingBy { it.first }.eachCount()
            return vendors.map { it.copy(productCount = stocked[it.id] ?: 0) }.filter { it.productCount > 0 }
        }

    val visibleProducts: List<StoredProduct>
        get() {
            var list = products
            if (inventoryReady) list = list.filter { getPresellStockBalance(stocks, it.sku) != null }
            val query = productSearch.trim().lowercase()
            if (query.isNotEmpty()) {
                list = list.filter { it.description.lowercase().contains(query) || it.sku.lowercase().contains(query) }
            }
            return sortPresellProductsByAvailability(list, stocks) { it.sku }
        }

    fun policyFor(product: StoredProduct): PresellProductInventoryPolicy =
        getPresellProductInventoryPolicy(getPresellStockBalance(stocks, product.sku), inventoryContext)

    fun cartQuantity(productId: String): Int = cart.firstOrNull { it.product.id == productId }?.quantity ?: 0
    fun requestQuantity(productId: String): Int = requests.firstOrNull { it.product.id == productId }?.quantity ?: 0

    val cartTotal: Double get() = cart.sumOf { it.product.price * it.quantity }
    val cartUnits: Int get() = cart.sumOf { it.quantity }

    val cartInventoryLines: List<InventoryCartItemLike>
        get() = cart.map { InventoryCartLine(it.product.sku, it.product.description, it.quantity) }

    val lanes: CheckoutReviewLanes get() = resolveCheckoutLanes(cart.size, requests.size)

    val cartAssessment: PresellCartInventoryAssessment
        get() = assessPresellCartInventory(cartInventoryLines, stocks, inventoryContext)

    /** Deferred requests can't be combined with lines reserved from expected stock. */
    val conflictDetected: Boolean
        get() = lanes.hasDeferredItems && cartContainsExpectedReservations(cartInventoryLines, stocks, inventoryContext)

    val requestBlocked: Boolean
        get() = inventoryContext.enforcementEnabled == true &&
            (inventoryContext.orderBlocked || !inventoryContext.controlsReady)
}
