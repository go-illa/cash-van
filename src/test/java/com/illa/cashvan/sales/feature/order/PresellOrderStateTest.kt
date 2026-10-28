package com.illa.cashvan.sales.feature.order

import com.illa.cashvan.sales.domain.inventory.PresellInventoryConfig
import com.illa.cashvan.sales.domain.inventory.PresellInventoryRoutingStatus
import com.illa.cashvan.sales.domain.inventory.PresellStockBalance
import com.illa.cashvan.sales.domain.inventory.PresellStockStatus
import com.illa.cashvan.sales.feature.cart.data.model.StoredCartLine
import com.illa.cashvan.sales.feature.cart.data.model.StoredProduct
import com.illa.cashvan.sales.feature.cart.data.model.StoredRequestLine
import com.illa.cashvan.sales.feature.catalog.data.mapper.PresellInventorySnapshot
import com.illa.cashvan.sales.feature.catalog.data.model.Vendor
import com.illa.cashvan.sales.feature.order.presentation.viewmodel.PresellOrderUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresellOrderStateTest {

    private fun product(id: String, sku: String, price: Double = 10.0) =
        StoredProduct(id = id, description = "Product $id", sku = sku, price = price, principal_id = "v1")

    private val inventory = PresellInventorySnapshot(
        config = PresellInventoryConfig(enforcementEnabled = true, routingStatus = PresellInventoryRoutingStatus.CONFIGURED),
        fetchedAt = "2026-10-27T08:00:00Z",
        isStale = false,
        stocks = mapOf(
            "A" to PresellStockBalance(sku = "A", status = PresellStockStatus.AVAILABLE, available = 5.0),
            "B" to PresellStockBalance(sku = "B", status = PresellStockStatus.OUT_OF_STOCK, available = 0.0)
        )
    )

    private val base = PresellOrderUiState(
        vendors = listOf(Vendor("v1", "Vendor 1", null, 3), Vendor("v2", "Vendor 2", null, 1)),
        vendorSkus = listOf("v1" to "A", "v1" to "B", "v1" to "C", "v2" to "D"),
        products = listOf(product("1", "A"), product("2", "B"), product("3", "C")),
        inventory = inventory
    )

    @Test
    fun `vendors are counted by stocked SKUs and hidden without any`() {
        val vendors = base.visibleVendors
        assertEquals(listOf("v1"), vendors.map { it.id })
        assertEquals(2, vendors.single().productCount)
    }

    @Test
    fun `before inventory loads every vendor and product shows`() {
        val loading = base.copy(inventory = null, inventoryLoading = true)
        assertEquals(2, loading.visibleVendors.size)
        assertEquals(3, loading.visibleProducts.size)
    }

    @Test
    fun `products without a balance are hidden and in-stock ones come first`() {
        assertEquals(listOf("A", "B"), base.visibleProducts.map { it.sku })
        assertEquals(listOf("B"), base.copy(productSearch = "product 2").visibleProducts.map { it.sku })
    }

    @Test
    fun `cart totals and lanes follow the baskets`() {
        val state = base.copy(
            cart = listOf(StoredCartLine(product("1", "A", 12.5), 2, "Vendor 1")),
            requests = listOf(StoredRequestLine(product("2", "B"), 3))
        )
        assertEquals(25.0, state.cartTotal, 0.0)
        assertEquals(2, state.cartUnits)
        assertTrue(state.lanes.hasCartItems && state.lanes.hasDeferredItems)
        assertFalse(state.conflictDetected)
        assertFalse(state.cartAssessment.blocked)
    }

    @Test
    fun `cart over the available balance is blocked`() {
        val state = base.copy(cart = listOf(StoredCartLine(product("1", "A"), 9, "Vendor 1")))
        assertTrue(state.cartAssessment.blocked)
    }
}
