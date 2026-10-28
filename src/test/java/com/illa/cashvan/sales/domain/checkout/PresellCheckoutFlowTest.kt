package com.illa.cashvan.sales.domain.checkout

import com.illa.cashvan.sales.domain.cart.PriceTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// DIS-446: truth tables for the presell checkout review confirm flow —
// guard precedence, disabled expression, and partial-failure retry semantics.
class PresellCheckoutFlowTest {

    private val combined = resolveCheckoutLanes(2, 1)
    private val orderOnly = resolveCheckoutLanes(2, 0)
    private val requestOnly = resolveCheckoutLanes(0, 2)

    private val openGuard = CheckoutConfirmGuardInput(
        lanes = combined,
        conflictDetected = false,
        locationReady = true,
        paymentType = PaymentType.CASH,
        hasFawryInvoice = false,
        cartBlocked = false,
        requestBlocked = false,
    )

    private fun blocked(reason: CheckoutConfirmBlockReason) = CheckoutConfirmGuard.Blocked(reason)

    // resolveConfirmGuard — reason precedence

    @Test
    fun `blocks with empty regardless of every other input`() {
        val guard = resolveConfirmGuard(
            openGuard.copy(
                lanes = resolveCheckoutLanes(0, 0),
                conflictDetected = true,
                cartBlocked = true,
                requestBlocked = true,
            ),
        )
        assertEquals(blocked(CheckoutConfirmBlockReason.EMPTY), guard)
    }

    @Test
    fun `conflict outranks the inventory blockers`() {
        assertEquals(blocked(CheckoutConfirmBlockReason.CONFLICT), resolveConfirmGuard(openGuard.copy(conflictDetected = true)))
        assertEquals(
            blocked(CheckoutConfirmBlockReason.CONFLICT),
            resolveConfirmGuard(openGuard.copy(conflictDetected = true, cartBlocked = true, requestBlocked = true)),
        )
    }

    @Test
    fun `missing location outranks the inventory blockers - order lanes only`() {
        assertEquals(
            blocked(CheckoutConfirmBlockReason.LOCATION),
            resolveConfirmGuard(openGuard.copy(lanes = orderOnly, locationReady = false, cartBlocked = true)),
        )
    }

    @Test
    fun `fawry without an invoice outranks cart-blocked`() {
        assertEquals(
            blocked(CheckoutConfirmBlockReason.FAWRY_INVOICE),
            resolveConfirmGuard(
                openGuard.copy(lanes = orderOnly, paymentType = PaymentType.FAWRY, hasFawryInvoice = false, cartBlocked = true),
            ),
        )
    }

    @Test
    fun `cart-blocked outranks request-blocked in combined flows`() {
        assertEquals(
            blocked(CheckoutConfirmBlockReason.CART_BLOCKED),
            resolveConfirmGuard(openGuard.copy(cartBlocked = true, requestBlocked = true)),
        )
    }

    @Test
    fun `request-blocked surfaces for request-only flows`() {
        assertEquals(
            blocked(CheckoutConfirmBlockReason.REQUEST_BLOCKED),
            resolveConfirmGuard(openGuard.copy(lanes = requestOnly, requestBlocked = true)),
        )
    }

    @Test
    fun `allows a healthy combined flow`() {
        assertEquals(CheckoutConfirmGuard.Allowed, resolveConfirmGuard(openGuard))
    }

    @Test
    fun `order-lane gates never block a request-only flow`() {
        // No GPS fix, no invoice, cart assessment blocked — none of these apply
        // when there is no sales order to place.
        assertEquals(
            CheckoutConfirmGuard.Allowed,
            resolveConfirmGuard(
                openGuard.copy(
                    lanes = requestOnly,
                    locationReady = false,
                    paymentType = PaymentType.FAWRY,
                    hasFawryInvoice = false,
                    cartBlocked = true,
                ),
            ),
        )
    }

    @Test
    fun `request-blocked never blocks an order-only flow`() {
        assertEquals(CheckoutConfirmGuard.Allowed, resolveConfirmGuard(openGuard.copy(lanes = orderOnly, requestBlocked = true)))
    }

    // isConfirmDisabled — disabled expression

    private val enabled = ConfirmDisabledInput(
        submitting = false,
        fawrySubmitting = false,
        requestSubmitting = false,
        reviewLocked = false,
        conflictDetected = false,
        lanes = combined,
        cartBlocked = false,
        requestBlocked = false,
        locationReady = true,
        paymentType = PaymentType.CASH,
        hasCollectionDate = true,
        hasFawryInvoice = true,
    )

    @Test
    fun `disables while any submission, lock, or conflict flag is set`() {
        listOf(
            "submitting" to enabled.copy(submitting = true),
            "fawrySubmitting" to enabled.copy(fawrySubmitting = true),
            "requestSubmitting" to enabled.copy(requestSubmitting = true),
            "reviewLocked" to enabled.copy(reviewLocked = true),
            "conflictDetected" to enabled.copy(conflictDetected = true),
        ).forEach { (name, input) ->
            assertTrue(name, isConfirmDisabled(input))
        }
    }

    @Test
    fun `is enabled for a healthy combined cash flow`() {
        assertFalse(isConfirmDisabled(enabled))
    }

    @Test
    fun `requires a collection date only for credit order lanes`() {
        val creditWithoutDate = enabled.copy(paymentType = PaymentType.CREDIT, hasCollectionDate = false)
        assertTrue(isConfirmDisabled(creditWithoutDate))
        assertFalse(isConfirmDisabled(creditWithoutDate.copy(lanes = requestOnly)))
    }

    @Test
    fun `requires an invoice only for fawry order lanes`() {
        val fawryWithoutInvoice = enabled.copy(paymentType = PaymentType.FAWRY, hasFawryInvoice = false)
        assertTrue(isConfirmDisabled(fawryWithoutInvoice))
        assertFalse(isConfirmDisabled(fawryWithoutInvoice.copy(lanes = requestOnly)))
    }

    @Test
    fun `requires location only for order lanes`() {
        val noLocation = enabled.copy(locationReady = false)
        assertTrue(isConfirmDisabled(noLocation))
        assertFalse(isConfirmDisabled(noLocation.copy(lanes = requestOnly)))
    }

    @Test
    fun `locks on the per-lane inventory blockers`() {
        assertTrue(isConfirmDisabled(enabled.copy(cartBlocked = true)))
        assertTrue(isConfirmDisabled(enabled.copy(requestBlocked = true)))
        // cartBlocked alone must not lock a request-only flow.
        assertFalse(isConfirmDisabled(enabled.copy(cartBlocked = true, lanes = requestOnly)))
    }

    // resolvePostSubmissionOutcome — partial-failure semantics

    private fun outcome(
        completeVisit: Boolean,
        showCelebration: Boolean,
        showRequestDialog: Boolean,
        clearCartLane: Boolean,
        clearRequestLane: Boolean,
        retryLeg: RetryLeg,
    ) = PostSubmissionOutcome(completeVisit, showCelebration, showRequestDialog, clearCartLane, clearRequestLane, retryLeg)

    private fun resolve(lanes: CheckoutReviewLanes, orderPlaced: Boolean, result: DeferredRequestOutcome) =
        resolvePostSubmissionOutcome(PostSubmissionOutcomeInput(lanes, orderPlaced, result))

    @Test
    fun `order-only success completes the visit with the celebration modal`() {
        assertEquals(
            outcome(true, true, false, true, false, RetryLeg.NONE),
            resolve(orderOnly, orderPlaced = true, DeferredRequestOutcome.NOT_ATTEMPTED),
        )
    }

    @Test
    fun `order-only failure keeps everything for a full retry`() {
        assertEquals(
            outcome(false, false, false, false, false, RetryLeg.FULL),
            resolve(orderOnly, orderPlaced = false, DeferredRequestOutcome.NOT_ATTEMPTED),
        )
    }

    @Test
    fun `request-only success completes with the no-order dialog`() {
        listOf(DeferredRequestOutcome.SUBMITTED, DeferredRequestOutcome.IDEMPOTENT_REPLAY).forEach { result ->
            assertEquals(
                result.wireValue,
                outcome(true, false, true, false, true, RetryLeg.NONE),
                resolve(requestOnly, orderPlaced = false, result),
            )
        }
    }

    @Test
    fun `request-only failure keeps the basket for a request retry`() {
        listOf(DeferredRequestOutcome.FAILED, DeferredRequestOutcome.ROUTING_FAILED).forEach { result ->
            assertEquals(
                result.wireValue,
                outcome(false, false, false, false, false, RetryLeg.REQUEST),
                resolve(requestOnly, orderPlaced = false, result),
            )
        }
    }

    @Test
    fun `request-only terminal_status completes and clears - retrying the same requestId can never succeed`() {
        // Same shape as success: the outcome clears the lane and arms no retry leg.
        assertEquals(
            outcome(true, false, true, false, true, RetryLeg.NONE),
            resolve(requestOnly, orderPlaced = false, DeferredRequestOutcome.TERMINAL_STATUS),
        )
    }

    @Test
    fun `combined with order placed and terminal request celebrates instead of arming a doomed request retry`() {
        assertEquals(
            outcome(true, true, false, true, true, RetryLeg.NONE),
            resolve(combined, orderPlaced = true, DeferredRequestOutcome.TERMINAL_STATUS),
        )
    }

    @Test
    fun `combined full success celebrates and clears both lanes - no dialog`() {
        assertEquals(
            outcome(true, true, false, true, true, RetryLeg.NONE),
            resolve(combined, orderPlaced = true, DeferredRequestOutcome.IDEMPOTENT_REPLAY),
        )
    }

    @Test
    fun `combined with order placed but request failed keeps the visit open for a request-only retry`() {
        assertEquals(
            // Order succeeded (its lane is cleared); the deferred basket stays for the idempotent retry.
            outcome(false, false, false, true, false, RetryLeg.REQUEST),
            resolve(combined, orderPlaced = true, DeferredRequestOutcome.FAILED),
        )
    }

    @Test
    fun `combined with order failed never started the request leg - full retry`() {
        assertEquals(
            outcome(false, false, false, false, false, RetryLeg.FULL),
            resolve(combined, orderPlaced = false, DeferredRequestOutcome.NOT_ATTEMPTED),
        )
    }

    // resolvePricingDrift — merchant tier/region drift detection

    private fun drift(liveTier: PriceTier, liveRegionId: String?, fetchedTier: PriceTier?, fetchedRegionId: String?) =
        resolvePricingDrift(PricingDriftInput(liveTier, liveRegionId, fetchedTier, fetchedRegionId))

    @Test
    fun `no drift when tier and region are unchanged`() {
        assertEquals(
            PricingDriftResult(drifted = false, tier = PriceTier.WHOLESALE, regionId = "region-1"),
            drift(PriceTier.WHOLESALE, "region-1", PriceTier.WHOLESALE, "region-1"),
        )
    }

    @Test
    fun `detects a tier change`() {
        assertEquals(
            PricingDriftResult(drifted = true, tier = PriceTier.WHOLESALE, regionId = "region-1"),
            drift(PriceTier.RETAIL, "region-1", PriceTier.WHOLESALE, "region-1"),
        )
    }

    @Test
    fun `detects a region change`() {
        assertEquals(
            PricingDriftResult(drifted = true, tier = PriceTier.RETAIL, regionId = "region-2"),
            drift(PriceTier.RETAIL, "region-1", PriceTier.RETAIL, "region-2"),
        )
    }

    @Test
    fun `detects tier and region changing together`() {
        assertEquals(
            PricingDriftResult(drifted = true, tier = PriceTier.BIG_WHOLESALE, regionId = "region-2"),
            drift(PriceTier.RETAIL, "region-1", PriceTier.BIG_WHOLESALE, "region-2"),
        )
    }

    @Test
    fun `detects the merchant losing its region - region cleared to null`() {
        assertEquals(
            PricingDriftResult(drifted = true, tier = PriceTier.RETAIL, regionId = null),
            drift(PriceTier.RETAIL, "region-1", PriceTier.RETAIL, null),
        )
    }

    @Test
    fun `normalizes a null fetched tier to retail - no drift against a retail flow`() {
        assertEquals(
            PricingDriftResult(drifted = false, tier = PriceTier.RETAIL, regionId = "region-1"),
            drift(PriceTier.RETAIL, "region-1", null, "region-1"),
        )
    }

    @Test
    fun `normalizing a null fetched tier can itself constitute drift`() {
        assertEquals(
            PricingDriftResult(drifted = true, tier = PriceTier.RETAIL, regionId = "region-1"),
            drift(PriceTier.WHOLESALE, "region-1", null, "region-1"),
        )
    }
}
