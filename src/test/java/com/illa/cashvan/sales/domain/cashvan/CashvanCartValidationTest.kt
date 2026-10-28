package com.illa.cashvan.sales.domain.cashvan

import org.junit.Assert.assertEquals
import org.junit.Test

// Ported from the PWA's src/pages/cashvanCartValidation.test.ts.
class CashvanCartValidationTest {

    private val priceDetails = CashvanPriceDetails(
        basePrice = 10.0,
        finalPrice = 10.0,
        vatPercentage = 0.0,
        vatAmount = 0.0,
        vatIsExclusive = false,
        discountAmount = 0.0,
        cashDiscount = 0.0,
    )

    private fun cartItem(planProductId: String) = CashvanCartItem(
        planProductId = planProductId,
        productId = "product-$planProductId",
        productName = "Product $planProductId",
        sku = "SKU-$planProductId",
        imageUrl = null,
        quantity = 1,
        maxQuantity = 5,
        unitPrice = 10.0,
        priceDetails = priceDetails,
    )

    private fun product(planProductId: String, hasValidPrice: Boolean = true, price: Double = 10.0) = CashvanProduct(
        planProductId = planProductId,
        productId = "product-$planProductId",
        productName = "Product $planProductId",
        sku = "SKU-$planProductId",
        imageUrl = null,
        assignedQuantity = 10,
        soldQuantity = 0,
        cashVanAvailable = 5,
        price = price,
        hasValidPrice = hasValidPrice,
        priceDetails = priceDetails,
    )

    @Test
    fun `returns cart items missing from the fetched products list`() {
        val invalidItems = findInvalidCashvanCartItems(
            items = listOf(cartItem("missing-product")),
            products = listOf(product("available-product")),
        )

        assertEquals(listOf("missing-product"), invalidItems.map { it.planProductId })
    }

    @Test
    fun `returns cart items whose fetched product no longer has a valid price`() {
        val invalidItems = findInvalidCashvanCartItems(
            items = listOf(cartItem("unpriced-product")),
            products = listOf(product("unpriced-product", hasValidPrice = false, price = 0.0)),
        )

        assertEquals(listOf("unpriced-product"), invalidItems.map { it.planProductId })
    }

    @Test
    fun `keeps cart items with a matching valid product`() {
        val invalidItems = findInvalidCashvanCartItems(
            items = listOf(cartItem("valid-product")),
            products = listOf(product("valid-product")),
        )

        assertEquals(emptyList<CashvanCartItem>(), invalidItems)
    }
}
