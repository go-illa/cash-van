package com.illa.cashvan.sales.domain.flow

import com.illa.cashvan.sales.domain.cart.PriceTier

/**
 * The TS `regionId?: string | null` field: a null [RegionRef] means the field
 * was absent (context incomplete), while `RegionRef(null)` means the merchant is
 * known to have no region.
 */
data class RegionRef(val id: String?)

/** Loosely-typed merchant context handed between presell screens or restored from storage. */
data class PresellFlowNavigationState(
    val merchantId: String? = null,
    val merchantName: String? = null,
    /** Null when absent or not one of the known tiers. */
    val priceTier: PriceTier? = null,
    val regionId: RegionRef? = null,
    val routeId: String? = null,
    val routeName: String? = null,
    val agentId: String? = null,
    val isAdHoc: Boolean? = null,
    val from: String? = null,
    val editCart: Boolean? = null,
)

data class PresellFlowContext(
    val merchantId: String,
    val merchantName: String,
    val priceTier: PriceTier,
    val regionId: String?,
    val routeId: String?,
    val routeName: String?,
    val agentId: String?,
    val isAdHoc: Boolean,
    val from: String,
    val editCart: Boolean,
)

private val PRESELL_FLOW_PATHS = setOf(
    "/vendor-selection",
    "/select-products",
    "/presell/checkout-review",
)

/**
 * Whether a PWA pathname is still inside the merchant's presell flow. Moving
 * BETWEEN these routes (products ↔ vendor selection ↔ checkout review of the
 * same merchant) preserves the cart and out-of-stock baskets; leaving the flow
 * wipes them. Query strings are not part of a pathname.
 */
fun isPresellFlowPath(pathname: String): Boolean = pathname in PRESELL_FLOW_PATHS

/**
 * A complete context, or null when the merchant id, a known price tier, or the
 * region field is missing. Optional fields get their defaults.
 */
fun normalizePresellFlowContext(value: PresellFlowNavigationState?): PresellFlowContext? {
    val merchantId = value?.merchantId
    if (merchantId.isNullOrEmpty()) return null
    val priceTier = value.priceTier ?: return null
    val region = value.regionId ?: return null

    return PresellFlowContext(
        merchantId = merchantId,
        merchantName = value.merchantName ?: "",
        priceTier = priceTier,
        regionId = region.id,
        routeId = value.routeId,
        routeName = value.routeName,
        agentId = value.agentId,
        isAdHoc = value.isAdHoc ?: false,
        from = value.from ?: "/",
        editCart = value.editCart ?: false,
    )
}

/** Prefers the current navigation state; falls back to the stored context (e.g. after process death). */
fun resolvePresellFlowContext(
    navigationState: PresellFlowNavigationState?,
    storedContext: PresellFlowNavigationState?,
): PresellFlowContext? = normalizePresellFlowContext(navigationState) ?: normalizePresellFlowContext(storedContext)
