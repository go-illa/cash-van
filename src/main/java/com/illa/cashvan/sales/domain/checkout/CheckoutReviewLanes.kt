package com.illa.cashvan.sales.domain.checkout

// DIS-446: lane resolution for the presell checkout review page. The page
// serves three entry combinations — an order (available products), a deferred
// out-of-stock request, or both — and every submission-path decision keys off
// this mode instead of ad-hoc checks.

enum class CheckoutReviewLaneMode(val wireValue: String) {
    EMPTY("empty"),
    ORDER_ONLY("order-only"),
    REQUEST_ONLY("request-only"),
    COMBINED("combined"),
}

data class CheckoutReviewLanes(
    val mode: CheckoutReviewLaneMode,
    val hasCartItems: Boolean,
    val hasDeferredItems: Boolean,
    val hasItems: Boolean,
)

fun resolveCheckoutLanes(cartItemCount: Int, requestItemCount: Int): CheckoutReviewLanes {
    val hasCartItems = cartItemCount > 0
    val hasDeferredItems = requestItemCount > 0
    val mode = when {
        hasCartItems && hasDeferredItems -> CheckoutReviewLaneMode.COMBINED
        hasCartItems -> CheckoutReviewLaneMode.ORDER_ONLY
        hasDeferredItems -> CheckoutReviewLaneMode.REQUEST_ONLY
        else -> CheckoutReviewLaneMode.EMPTY
    }
    return CheckoutReviewLanes(
        mode = mode,
        hasCartItems = hasCartItems,
        hasDeferredItems = hasDeferredItems,
        hasItems = hasCartItems || hasDeferredItems,
    )
}
