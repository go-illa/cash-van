package com.illa.cashvan.sales.domain.cart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

// DIS-446: truth tables for the cart repricing mapper — replace-everything
// semantics that heal tier/region drift and mid-visit price edits.
class CartPriceRefreshTest {

    private fun priceRow(
        productId: String = "product-1",
        finalPrice: Double? = 90.0,
    ) = ProductPriceRow(
        id = "price-1",
        productId = productId,
        priceTier = PriceTier.WHOLESALE,
        price = 100.0,
        basePrice = 100.0,
        finalPrice = finalPrice,
        discounted = true,
        discountDetails = null,
        vatPercentage = 14.0,
        vatApplication = 1,
        startDate = null,
        endDate = null,
        active = true,
        discountType = 1,
        cashDiscount = 5.0,
        governorateId = "region-1",
        createdAt = "2026-01-01",
        updatedAt = "2026-01-01",
    )

    private val baseProduct = CartProduct(
        id = "product-1",
        description = "منتج أول",
        sku = "SKU-1",
        price = 120.0, // stale retail price
        principalId = null,
        imageUrl = null,
        priceInfo = priceRow(finalPrice = 120.0),
    )

    private fun cartItem(productId: String = "product-1", product: CartProduct = baseProduct) = CartItem(
        productId = productId,
        quantity = 2,
        product = product,
        principalName = "Vendor",
    )

    @Test
    fun `reprices every item that has a fresh row, replacing price and price_info`() {
        val (updatedItems, unfixableDescriptions) = applyFreshPrices(listOf(cartItem()), listOf(priceRow()))

        assertEquals(emptyList<String>(), unfixableDescriptions)
        assertEquals(90.0, updatedItems[0].product.price, 0.0) // final_price of the fresh row
        assertEquals(priceRow(), updatedItems[0].product.priceInfo)
        // Non-price fields survive untouched.
        assertEquals(2, updatedItems[0].quantity)
        assertEquals("SKU-1", updatedItems[0].product.sku)
    }

    @Test
    fun `flags items with no row under the tier or region as unfixable and keeps them as-is`() {
        val priced = cartItem(productId = "product-1")
        val unpriced = cartItem(
            productId = "product-2",
            product = baseProduct.copy(id = "product-2", description = "منتج ثانٍ"),
        )
        val (updatedItems, unfixableDescriptions) = applyFreshPrices(
            listOf(priced, unpriced),
            listOf(priceRow(productId = "product-1")),
        )

        assertEquals(listOf("منتج ثانٍ"), unfixableDescriptions)
        assertSame(unpriced, updatedItems[1]) // untouched, same reference
        assertEquals(90.0, updatedItems[0].product.price, 0.0)
    }

    @Test
    fun `returns an empty result for an empty cart`() {
        assertEquals(
            FreshPricesResult(updatedItems = emptyList(), unfixableDescriptions = emptyList()),
            applyFreshPrices(emptyList(), listOf(priceRow())),
        )
    }

    @Test
    fun `treats duplicate rows deterministically - the last row per product wins`() {
        val (updatedItems) = applyFreshPrices(
            listOf(cartItem()),
            listOf(priceRow(finalPrice = 80.0), priceRow(finalPrice = 70.0)),
        )
        assertEquals(70.0, updatedItems[0].product.price, 0.0)
    }

    @Test
    fun `uses 0 when the fresh row has a null final_price - query-level filter normally prevents this`() {
        val (updatedItems) = applyFreshPrices(listOf(cartItem()), listOf(priceRow(finalPrice = null)))
        assertEquals(0.0, updatedItems[0].product.price, 0.0)
    }
}
