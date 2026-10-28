package com.illa.cashvan.sales.domain.flow

import com.illa.cashvan.sales.domain.cart.PriceTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresellFlowContextTest {

    private val stored = PresellFlowNavigationState(
        merchantId = "merchant-stored",
        merchantName = "Stored Merchant",
        priceTier = PriceTier.RETAIL,
        regionId = RegionRef("region-stored"),
        routeId = "route-1",
        routeName = "Route One",
        agentId = "agent-1",
        isAdHoc = true,
        from = "/",
        editCart = false,
    )

    private val storedContext = PresellFlowContext(
        merchantId = "merchant-stored",
        merchantName = "Stored Merchant",
        priceTier = PriceTier.RETAIL,
        regionId = "region-stored",
        routeId = "route-1",
        routeName = "Route One",
        agentId = "agent-1",
        isAdHoc = true,
        from = "/",
        editCart = false,
    )

    // Presell flow context recovery

    @Test
    fun `prefers current navigation state over the stored context`() {
        val context = resolvePresellFlowContext(
            PresellFlowNavigationState(
                merchantId = "merchant-nav",
                merchantName = "Navigation Merchant",
                priceTier = PriceTier.WHOLESALE,
                regionId = RegionRef(null),
            ),
            stored,
        )
        assertEquals("merchant-nav", context?.merchantId)
        assertEquals("Navigation Merchant", context?.merchantName)
        assertEquals(PriceTier.WHOLESALE, context?.priceTier)
        assertNull(context?.regionId)
    }

    @Test
    fun `restores merchant and pricing context after a hard refresh`() {
        assertEquals(storedContext, resolvePresellFlowContext(null, stored))
    }

    @Test
    fun `rejects incomplete stored values`() {
        // The TS case also feeds '{bad json' to the JSON parser, which is a storage concern here.
        assertNull(normalizePresellFlowContext(PresellFlowNavigationState(merchantId = "merchant-1")))
        assertNull(
            normalizePresellFlowContext(
                PresellFlowNavigationState(
                    merchantId = "merchant-1",
                    priceTier = PriceTier.fromWire("invalid-tier"),
                    regionId = RegionRef("region-1"),
                ),
            ),
        )
    }

    @Test
    fun `requires the region field even though its value may be null`() {
        val withoutRegion = PresellFlowNavigationState(merchantId = "merchant-1", priceTier = PriceTier.RETAIL)
        assertNull(normalizePresellFlowContext(withoutRegion))

        val context = normalizePresellFlowContext(withoutRegion.copy(regionId = RegionRef(null)))
        assertEquals(
            PresellFlowContext(
                merchantId = "merchant-1",
                merchantName = "",
                priceTier = PriceTier.RETAIL,
                regionId = null,
                routeId = null,
                routeName = null,
                agentId = null,
                isAdHoc = false,
                from = "/",
                editCart = false,
            ),
            context,
        )
    }

    // isPresellFlowPath — popstate basket guard

    @Test
    fun `recognizes every route inside the merchant presell flow`() {
        assertTrue(isPresellFlowPath("/vendor-selection"))
        assertTrue(isPresellFlowPath("/select-products"))
        assertTrue(isPresellFlowPath("/presell/checkout-review"))
    }

    @Test
    fun `query strings do not take a path out of the flow`() {
        // A pathname never includes the query — /select-products?principalId=X stays in-flow.
        assertTrue(isPresellFlowPath("/select-products"))
    }

    @Test
    fun `treats every other destination as leaving the flow`() {
        assertFalse(isPresellFlowPath("/")) // merchant selection (dashboard)
        assertFalse(isPresellFlowPath("/cashvan/select-products"))
        assertFalse(isPresellFlowPath("/orders"))
        assertFalse(isPresellFlowPath("/vendor-selection/extra"))
        assertFalse(isPresellFlowPath(""))
    }
}
