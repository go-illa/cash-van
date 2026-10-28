package com.illa.cashvan.sales.domain.checkout

import org.junit.Assert.assertEquals
import org.junit.Test

class CheckoutReviewLanesTest {

    @Test
    fun `classifies the vendor-selection cart-only path as order-only`() {
        assertEquals(
            CheckoutReviewLanes(CheckoutReviewLaneMode.ORDER_ONLY, hasCartItems = true, hasDeferredItems = false, hasItems = true),
            resolveCheckoutLanes(3, 0),
        )
    }

    @Test
    fun `classifies the deferred-only path with no sales order`() {
        assertEquals(
            CheckoutReviewLanes(CheckoutReviewLaneMode.REQUEST_ONLY, hasCartItems = false, hasDeferredItems = true, hasItems = true),
            resolveCheckoutLanes(0, 2),
        )
    }

    @Test
    fun `classifies the combined available + deferred checkout`() {
        assertEquals(
            CheckoutReviewLanes(CheckoutReviewLaneMode.COMBINED, hasCartItems = true, hasDeferredItems = true, hasItems = true),
            resolveCheckoutLanes(2, 4),
        )
    }

    @Test
    fun `flags both baskets empty - the direct-visit redirect condition`() {
        assertEquals(
            CheckoutReviewLanes(CheckoutReviewLaneMode.EMPTY, hasCartItems = false, hasDeferredItems = false, hasItems = false),
            resolveCheckoutLanes(0, 0),
        )
    }
}
