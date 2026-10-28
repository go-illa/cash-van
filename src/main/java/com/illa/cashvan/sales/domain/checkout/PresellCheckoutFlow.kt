package com.illa.cashvan.sales.domain.checkout

import com.illa.cashvan.sales.domain.cart.PriceTier

// DIS-446: decision logic for the presell checkout review page's confirm flow —
// the guard ladder, the disabled expression, the partial-failure outcome
// semantics, and merchant pricing drift detection.

enum class CheckoutConfirmBlockReason(val wireValue: String) {
    EMPTY("empty"),
    CONFLICT("conflict"),
    LOCATION("location"),
    FAWRY_INVOICE("fawry-invoice"),
    CART_BLOCKED("cart-blocked"),
    REQUEST_BLOCKED("request-blocked"),
}

sealed interface CheckoutConfirmGuard {
    data object Allowed : CheckoutConfirmGuard

    data class Blocked(val reason: CheckoutConfirmBlockReason) : CheckoutConfirmGuard
}

data class CheckoutConfirmGuardInput(
    val lanes: CheckoutReviewLanes,
    /** Deferred items mixed with a cart that reserves from expected stock. */
    val conflictDetected: Boolean,
    val locationReady: Boolean,
    val paymentType: PaymentType,
    val hasFawryInvoice: Boolean,
    val cartBlocked: Boolean,
    val requestBlocked: Boolean,
)

/**
 * The confirm-handler guard ladder, in the exact order the page applies it so
 * the message the agent sees is deterministic. EMPTY, LOCATION and
 * FAWRY_INVOICE are silent early-outs (the confirm button is disabled for
 * them); the other reasons carry a user-facing message.
 */
fun resolveConfirmGuard(input: CheckoutConfirmGuardInput): CheckoutConfirmGuard {
    val lanes = input.lanes
    val reason = when {
        !lanes.hasItems -> CheckoutConfirmBlockReason.EMPTY
        input.conflictDetected -> CheckoutConfirmBlockReason.CONFLICT
        lanes.hasCartItems && !input.locationReady -> CheckoutConfirmBlockReason.LOCATION
        lanes.hasCartItems && input.paymentType == PaymentType.FAWRY && !input.hasFawryInvoice ->
            CheckoutConfirmBlockReason.FAWRY_INVOICE
        lanes.hasCartItems && input.cartBlocked -> CheckoutConfirmBlockReason.CART_BLOCKED
        lanes.hasDeferredItems && input.requestBlocked -> CheckoutConfirmBlockReason.REQUEST_BLOCKED
        else -> null
    }
    return reason?.let { CheckoutConfirmGuard.Blocked(it) } ?: CheckoutConfirmGuard.Allowed
}

data class ConfirmDisabledInput(
    val submitting: Boolean,
    val fawrySubmitting: Boolean,
    val requestSubmitting: Boolean,
    /** Inventory still loading/refreshing — every control is locked. */
    val reviewLocked: Boolean,
    val conflictDetected: Boolean,
    val lanes: CheckoutReviewLanes,
    val cartBlocked: Boolean,
    val requestBlocked: Boolean,
    val locationReady: Boolean,
    val paymentType: PaymentType,
    val hasCollectionDate: Boolean,
    val hasFawryInvoice: Boolean,
)

fun isConfirmDisabled(input: ConfirmDisabledInput): Boolean {
    val lanes = input.lanes
    return input.submitting ||
        input.fawrySubmitting ||
        input.requestSubmitting ||
        input.reviewLocked ||
        input.conflictDetected ||
        (lanes.hasCartItems && input.cartBlocked) ||
        (lanes.hasDeferredItems && input.requestBlocked) ||
        (lanes.hasCartItems && !input.locationReady) ||
        (lanes.hasCartItems && input.paymentType == PaymentType.CREDIT && !input.hasCollectionDate) ||
        (lanes.hasCartItems && input.paymentType == PaymentType.FAWRY && !input.hasFawryInvoice)
}

/** Result of the deferred out-of-stock request leg, if it ran. */
enum class DeferredRequestOutcome(val wireValue: String) {
    NOT_ATTEMPTED("not_attempted"),
    SUBMITTED("submitted"),
    IDEMPOTENT_REPLAY("idempotent_replay"),
    TERMINAL_STATUS("terminal_status"),
    FAILED("failed"),
    ROUTING_FAILED("routing_failed"),
}

data class PostSubmissionOutcomeInput(
    val lanes: CheckoutReviewLanes,
    /** False when the sales-order leg failed (or never ran because it doesn't exist). */
    val orderPlaced: Boolean,
    val deferredRequestResult: DeferredRequestOutcome,
)

/**
 * What the agent must retry after a partial failure: FULL re-runs the whole
 * confirm (the order leg failed, so the request leg never started); REQUEST
 * retries only the deferred leg with the same persisted requestId (idempotent replay).
 */
enum class RetryLeg(val wireValue: String) {
    NONE("none"),
    FULL("full"),
    REQUEST("request"),
}

data class PostSubmissionOutcome(
    /** Both lanes finished — the visit scope can be closed. */
    val completeVisit: Boolean,
    /** Celebration modal for flows that placed a sales order. */
    val showCelebration: Boolean,
    /** Request-only confirmation dialog ("no order created, no delivery"). */
    val showRequestDialog: Boolean,
    /** The order lane succeeded — its cart can be cleared now. */
    val clearCartLane: Boolean,
    /** The request lane succeeded — the deferred basket can be cleared now. */
    val clearRequestLane: Boolean,
    val retryLeg: RetryLeg,
)

// TERMINAL_STATUS counts as done: the request was already placed/rejected by
// telesales, so there is nothing left to retry — the local basket must be
// cleared and the visit completed instead of arming a retry leg that can never succeed.
private fun DeferredRequestOutcome.succeeded(): Boolean =
    this == DeferredRequestOutcome.SUBMITTED ||
        this == DeferredRequestOutcome.IDEMPOTENT_REPLAY ||
        this == DeferredRequestOutcome.TERMINAL_STATUS

fun resolvePostSubmissionOutcome(input: PostSubmissionOutcomeInput): PostSubmissionOutcome {
    val lanes = input.lanes
    val requestLegSucceeded = input.deferredRequestResult.succeeded()
    val orderSucceeded = !lanes.hasCartItems || input.orderPlaced
    val requestSucceeded = !lanes.hasDeferredItems || requestLegSucceeded
    val completeVisit = (lanes.hasCartItems || lanes.hasDeferredItems) && orderSucceeded && requestSucceeded

    return PostSubmissionOutcome(
        completeVisit = completeVisit,
        showCelebration = completeVisit && lanes.hasCartItems && input.orderPlaced,
        showRequestDialog = completeVisit && !lanes.hasCartItems,
        clearCartLane = lanes.hasCartItems && input.orderPlaced,
        clearRequestLane = lanes.hasDeferredItems && requestLegSucceeded,
        retryLeg = when {
            !orderSucceeded -> RetryLeg.FULL
            lanes.hasDeferredItems && !requestSucceeded -> RetryLeg.REQUEST
            else -> RetryLeg.NONE
        },
    )
}

data class PricingDriftInput(
    /** Tier the checkout currently believes applies (state/live tracker). */
    val liveTier: PriceTier,
    val liveRegionId: String?,
    /** Values just fetched from the merchants row; a null tier means retail. */
    val fetchedTier: PriceTier?,
    val fetchedRegionId: String?,
)

data class PricingDriftResult(
    /** True when the merchant's tier/region changed since the flow last knew. */
    val drifted: Boolean,
    /** The normalized fresh values the order must be priced/submitted with. */
    val tier: PriceTier,
    val regionId: String?,
)

/**
 * Compares the merchant's just-fetched pricing coordinates against the ones
 * the checkout flow is holding. A drift means every cart item may carry prices
 * from the wrong tier/region — the caller must reprice and block the in-flight
 * submission so the agent confirms the updated totals explicitly. A null
 * fetched tier normalizes to RETAIL (merchants.price_tier convention).
 */
fun resolvePricingDrift(input: PricingDriftInput): PricingDriftResult {
    val tier = input.fetchedTier ?: PriceTier.RETAIL
    val regionId = input.fetchedRegionId
    return PricingDriftResult(
        drifted = tier != input.liveTier || regionId != input.liveRegionId,
        tier = tier,
        regionId = regionId,
    )
}
