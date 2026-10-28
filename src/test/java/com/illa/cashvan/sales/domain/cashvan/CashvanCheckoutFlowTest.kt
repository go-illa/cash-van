package com.illa.cashvan.sales.domain.cashvan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported from the PWA's src/pages/cashvanCheckoutFlow.test.ts. The
// sessionStorage wrappers are not ported; their parse/serialize cores are
// tested directly instead.
class CashvanCheckoutFlowTest {

    private val merchantOne = CashvanMerchantContext(
        merchantId = "merchant-1",
        merchantName = "Merchant One",
        governorate = null,
        priceTier = "retail",
    )

    // resolveCheckoutGuardRedirect

    @Test
    fun `does not redirect after a successful order submission clears the cart`() {
        assertNull(resolveCheckoutGuardRedirect(CheckoutGuardInput(itemCount = 0, isSuccess = true, merchantId = "merchant-1")))
    }

    @Test
    fun `redirects missing merchant context to merchant selection`() {
        assertEquals(
            CashvanRouteRedirect(CashvanRoute.MERCHANTS),
            resolveCheckoutGuardRedirect(CheckoutGuardInput(itemCount = 1, isSuccess = false)),
        )
    }

    @Test
    fun `keeps full merchant pricing context when an empty cart returns to product selection`() {
        assertEquals(
            CashvanRouteRedirect(CashvanRoute.SELECT_PRODUCTS, merchantOne),
            resolveCheckoutGuardRedirect(
                CheckoutGuardInput(
                    itemCount = 0,
                    isSuccess = false,
                    merchantId = "merchant-1",
                    merchantName = "Merchant One",
                    governorate = GovernorateRef(null),
                    priceTier = "retail",
                ),
            ),
        )
    }

    @Test
    fun `redirects to merchant selection when an empty cart has incomplete pricing context`() {
        assertEquals(
            CashvanRouteRedirect(CashvanRoute.MERCHANTS),
            resolveCheckoutGuardRedirect(
                CheckoutGuardInput(itemCount = 0, isSuccess = false, merchantId = "merchant-1", merchantName = "Merchant One"),
            ),
        )
    }

    // validateRebateInput

    @Test
    fun `allows empty and partial decimal values while deriving a numeric rebate`() {
        assertEquals(RebateValidation(0.0), validateRebateInput("", 100.0))
        assertEquals(RebateValidation(0.0), validateRebateInput("0.", 100.0))
        assertEquals(RebateValidation(12.5), validateRebateInput("12.5", 100.0))
    }

    @Test
    fun `rejects negative rebates`() {
        assertEquals(RebateValidation(-1.0, "قيمة الخصم لا يمكن أن تكون سالبة"), validateRebateInput("-1", 100.0))
    }

    @Test
    fun `rejects rebates greater than the order total before rebate`() {
        assertEquals(RebateValidation(101.0, "قيمة الخصم لا يمكن أن تتجاوز إجمالي الطلب"), validateRebateInput("101", 100.0))
    }

    // Kotlin-only: unparseable input keeps the PWA's NaN and invalid-value message.
    @Test
    fun `rejects unparseable and infinite rebates`() {
        val invalid = validateRebateInput("12abc", 100.0)
        assertTrue(invalid.rebateValue.isNaN())
        assertEquals("قيمة الخصم غير صالحة", invalid.errorMessage)

        assertEquals("قيمة الخصم غير صالحة", validateRebateInput("Infinity", 100.0).errorMessage)
        assertEquals(RebateValidation(16.0), validateRebateInput(" 0x10 ", 100.0))
    }

    // Merchant context recovery

    @Test
    fun `uses navigation state before stored session context`() {
        assertEquals(
            CashvanMerchantContext("nav-merchant", "Navigation Merchant", null, "retail"),
            resolveCashvanMerchantContext(
                CashvanCheckoutNavigationState("nav-merchant", "Navigation Merchant", GovernorateRef(null), "retail"),
                CashvanCheckoutNavigationState("stored-merchant", "Stored Merchant", GovernorateRef("gov-1"), "wholesale"),
            ),
        )
    }

    @Test
    fun `falls back to stored session context after refresh`() {
        assertEquals(
            CashvanMerchantContext("stored-merchant", "Stored Merchant", "gov-1", "wholesale"),
            resolveCashvanMerchantContext(
                null,
                CashvanCheckoutNavigationState("stored-merchant", "Stored Merchant", GovernorateRef("gov-1"), "wholesale"),
            ),
        )
    }

    @Test
    fun `ignores malformed stored session context`() {
        assertNull(parseCashvanMerchantContext("{bad json"))
        assertNull(parseCashvanMerchantContext("""{"merchantId":"merchant-1","merchantName":"Merchant One"}"""))
    }

    // Kotlin-only: the persisted JSON round-trips and matches JSON.stringify's output.
    @Test
    fun `serializes the context like JSON stringify and parses it back`() {
        val state = CashvanCheckoutNavigationState("merchant-1", "n \"q\" \\ \n", GovernorateRef(null), "retail")
        val json = serializeCashvanMerchantContext(state)

        assertEquals("""{"merchantId":"merchant-1","merchantName":"n \"q\" \\ \n","governorate":null,"priceTier":"retail"}""", json)
        assertEquals(CashvanMerchantContext("merchant-1", "n \"q\" \\ \n", null, "retail"), parseCashvanMerchantContext(json))
        assertEquals(
            CashvanMerchantContext("m", "", "gov-1", "retail"),
            parseCashvanMerchantContext("""{"merchantId":"m","governorate":"gov-1","priceTier":"retail"}"""),
        )
        assertNull(serializeCashvanMerchantContext(CashvanCheckoutNavigationState(merchantId = "m", priceTier = "retail")))
        assertNull(parseCashvanMerchantContext("[1,2]"))
        assertNull(parseCashvanMerchantContext(""))
        assertNull(parseCashvanMerchantContext("""{"merchantId":"","governorate":null,"priceTier":"retail"}"""))
    }

    // Merchant scroll offset

    @Test
    fun `reads a non-negative numeric offset and ignores invalid data`() {
        assertEquals(420.0, parseCashvanMerchantScrollOffset("420") ?: Double.NaN, 0.0)
        assertNull(parseCashvanMerchantScrollOffset("-1"))
        assertNull(parseCashvanMerchantScrollOffset("abc"))
        assertNull(parseCashvanMerchantScrollOffset(null))
    }

    @Test
    fun `restores in view-all mode once paging data is no longer loading`() {
        assertFalse(
            canRestoreCashvanMerchantScroll(
                CashvanMerchantScrollRestoreInput(viewAll = true, pagedLoading = true, nearbyLoading = false, hasQueryLocation = false, hasNearbyError = false),
            ),
        )
        assertTrue(
            canRestoreCashvanMerchantScroll(
                CashvanMerchantScrollRestoreInput(viewAll = true, pagedLoading = false, nearbyLoading = true, hasQueryLocation = false, hasNearbyError = false),
            ),
        )
    }

    @Test
    fun `waits in nearby mode until loading is done and data preconditions exist`() {
        assertFalse(
            canRestoreCashvanMerchantScroll(
                CashvanMerchantScrollRestoreInput(viewAll = false, pagedLoading = false, nearbyLoading = true, hasQueryLocation = true, hasNearbyError = false),
            ),
        )
        assertFalse(
            canRestoreCashvanMerchantScroll(
                CashvanMerchantScrollRestoreInput(viewAll = false, pagedLoading = false, nearbyLoading = false, hasQueryLocation = false, hasNearbyError = false),
            ),
        )
    }

    @Test
    fun `allows nearby restoration after fetch settles with either location or error`() {
        assertTrue(
            canRestoreCashvanMerchantScroll(
                CashvanMerchantScrollRestoreInput(viewAll = false, pagedLoading = false, nearbyLoading = false, hasQueryLocation = true, hasNearbyError = false),
            ),
        )
        assertTrue(
            canRestoreCashvanMerchantScroll(
                CashvanMerchantScrollRestoreInput(viewAll = false, pagedLoading = false, nearbyLoading = false, hasQueryLocation = false, hasNearbyError = true),
            ),
        )
    }

    @Test
    fun `writes rounded non-negative offsets and ignores invalid values`() {
        val written = listOf(123.7, -1.0, Double.NaN).mapNotNull(::serializeCashvanMerchantScrollOffset)
        assertEquals(listOf("124"), written)
    }

    // resolveCashvanProductSelectionGuard

    @Test
    fun `redirects stale completed order history entries to the dashboard`() {
        assertEquals(CashvanRouteRedirect(CashvanRoute.HOME), resolveCashvanProductSelectionGuard(merchantOne, orderCompleted = true))
    }

    @Test
    fun `product selection redirects missing merchant context to merchant selection`() {
        assertEquals(CashvanRouteRedirect(CashvanRoute.MERCHANTS), resolveCashvanProductSelectionGuard(null, orderCompleted = false))
    }

    // resolveCashvanBackNavigation

    @Test
    fun `routes product selection back to merchant selection and clears the draft`() {
        assertEquals(
            CashvanBackNavigation(route = CashvanRoute.MERCHANTS, clearCart = true, clearMerchantContext = true),
            resolveCashvanBackNavigation(CashvanFlowScreen.PRODUCT_SELECTION, null),
        )
    }

    @Test
    fun `routes checkout back to product selection with restored merchant context`() {
        assertEquals(
            CashvanBackNavigation(route = CashvanRoute.SELECT_PRODUCTS, state = merchantOne, clearCart = false, clearMerchantContext = false),
            resolveCashvanBackNavigation(CashvanFlowScreen.CHECKOUT, merchantOne),
        )
    }

    // resolveCashvanMerchantBackAction

    @Test
    fun `closes the no-order reason sheet before leaving the merchant page`() {
        assertEquals(
            CashvanMerchantBackAction.CLOSE_NO_ORDER_REASON,
            resolveCashvanMerchantBackAction(showNoOrderReason = true, showVisitTypeSheet = true),
        )
    }

    @Test
    fun `closes the visit type sheet before leaving the merchant page`() {
        assertEquals(
            CashvanMerchantBackAction.CLOSE_VISIT_TYPE,
            resolveCashvanMerchantBackAction(showNoOrderReason = false, showVisitTypeSheet = true),
        )
    }

    @Test
    fun `navigates home when no merchant sheet is open`() {
        assertEquals(
            CashvanMerchantBackAction.NAVIGATE_HOME,
            resolveCashvanMerchantBackAction(showNoOrderReason = false, showVisitTypeSheet = false),
        )
    }

    // Device back guard state

    @Test
    fun `detects whether the current history entry already guards this pathname`() {
        assertTrue(hasCashvanDeviceBackGuard(mapOf("cashvanBackGuardPath" to "/cashvan/checkout"), "/cashvan/checkout"))
        assertFalse(hasCashvanDeviceBackGuard(mapOf("cashvanBackGuardPath" to "/cashvan/select-products"), "/cashvan/checkout"))
    }

    @Test
    fun `preserves existing router state while adding the cashvan guard marker`() {
        assertEquals(
            mapOf(
                "usr" to mapOf("merchantId" to "merchant-1"),
                "key" to "abc",
                "cashvanBackGuardPath" to "/cashvan/checkout",
            ),
            createCashvanDeviceBackState(mapOf("usr" to mapOf("merchantId" to "merchant-1"), "key" to "abc"), "/cashvan/checkout"),
        )
    }
}
