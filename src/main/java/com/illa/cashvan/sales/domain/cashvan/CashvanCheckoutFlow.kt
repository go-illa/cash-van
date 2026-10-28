package com.illa.cashvan.sales.domain.cashvan

import com.illa.cashvan.sales.domain.format.jsMathRound
import com.illa.cashvan.sales.domain.format.jsNumberToString
import com.illa.cashvan.sales.domain.format.jsTrim
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Navigation guards and input validation for the CashVan merchant → products →
// checkout flow. The PWA's sessionStorage reads/writes are not ported; their
// pure parse/serialize halves are, so a persisted value behaves the same.

const val ACTIVE_CASHVAN_MERCHANT_KEY = "cashvan_active_merchant_context"
const val CASHVAN_ORDER_COMPLETED_KEY = "cashvan_order_completed"
const val CASHVAN_MERCHANT_SCROLL_OFFSET_KEY = "cashvan_merchant_scroll_offset"
const val CASHVAN_BACK_GUARD_PATH_KEY = "cashvanBackGuardPath"

const val REBATE_INVALID_MESSAGE = "قيمة الخصم غير صالحة"
const val REBATE_NEGATIVE_MESSAGE = "قيمة الخصم لا يمكن أن تكون سالبة"
const val REBATE_EXCEEDS_TOTAL_MESSAGE = "قيمة الخصم لا يمكن أن تتجاوز إجمالي الطلب"

/**
 * The TS `governorate?: string | null` field: a null [GovernorateRef] means the
 * field was absent (context incomplete), `GovernorateRef(null)` means the
 * merchant is known to have no governorate.
 */
data class GovernorateRef(val id: String?)

/** Loosely-typed merchant context handed between CashVan screens or restored from storage. */
data class CashvanCheckoutNavigationState(
    val merchantId: String? = null,
    val merchantName: String? = null,
    val governorate: GovernorateRef? = null,
    /** Any non-empty tier string is accepted, as in the PWA (not validated against PriceTier). */
    val priceTier: String? = null,
)

data class CashvanMerchantContext(
    val merchantId: String,
    val merchantName: String,
    /** Governorate id, or null when the merchant has none. */
    val governorate: String?,
    val priceTier: String,
)

enum class CashvanRoute(val path: String) {
    HOME("/"),
    MERCHANTS("/cashvan/merchants"),
    SELECT_PRODUCTS("/cashvan/select-products"),
}

data class CashvanRouteRedirect(
    val route: CashvanRoute,
    val state: CashvanMerchantContext? = null,
)

data class CashvanBackNavigation(
    val route: CashvanRoute,
    val state: CashvanMerchantContext? = null,
    val clearCart: Boolean,
    val clearMerchantContext: Boolean,
)

enum class CashvanFlowScreen(val wireValue: String) {
    PRODUCT_SELECTION("product-selection"),
    CHECKOUT("checkout"),
}

enum class CashvanMerchantBackAction(val wireValue: String) {
    CLOSE_NO_ORDER_REASON("close-no-order-reason"),
    CLOSE_VISIT_TYPE("close-visit-type"),
    NAVIGATE_HOME("navigate-home"),
}

data class CheckoutGuardInput(
    val itemCount: Int,
    val isSuccess: Boolean,
    val merchantId: String? = null,
    val merchantName: String? = null,
    val governorate: GovernorateRef? = null,
    val priceTier: String? = null,
)

data class CashvanMerchantScrollRestoreInput(
    val viewAll: Boolean,
    val pagedLoading: Boolean,
    val nearbyLoading: Boolean,
    val hasQueryLocation: Boolean,
    val hasNearbyError: Boolean,
)

data class RebateValidation(
    /** NaN for unparseable input, like the PWA's `Number(input)`. */
    val rebateValue: Double,
    val errorMessage: String? = null,
)

/** A complete context, or null when the merchant id, price tier or governorate field is missing. */
fun normalizeCashvanMerchantContext(value: CashvanCheckoutNavigationState?): CashvanMerchantContext? {
    val merchantId = value?.merchantId
    if (merchantId.isNullOrEmpty()) return null
    val priceTier = value.priceTier
    if (priceTier.isNullOrEmpty()) return null
    val governorate = value.governorate ?: return null

    return CashvanMerchantContext(
        merchantId = merchantId,
        merchantName = value.merchantName ?: "",
        governorate = governorate.id,
        priceTier = priceTier,
    )
}

/**
 * Parses a context persisted by [serializeCashvanMerchantContext]. Malformed
 * JSON, a non-object, or an incomplete context yields null. Non-string field
 * values count as absent (the PWA would pass them through untyped).
 */
fun parseCashvanMerchantContext(raw: String?): CashvanMerchantContext? {
    if (raw.isNullOrEmpty()) return null

    val json = try {
        Json.parseToJsonElement(raw)
    } catch (_: IllegalArgumentException) {
        return null
    }
    val obj = json as? JsonObject ?: return null

    fun JsonElement?.stringOrNull(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

    val governorate = when (val element = obj["governorate"]) {
        null -> null
        JsonNull -> GovernorateRef(null)
        else -> element.stringOrNull()?.let(::GovernorateRef)
    }

    return normalizeCashvanMerchantContext(
        CashvanCheckoutNavigationState(
            merchantId = obj["merchantId"].stringOrNull(),
            merchantName = obj["merchantName"].stringOrNull(),
            governorate = governorate,
            priceTier = obj["priceTier"].stringOrNull(),
        ),
    )
}

/** The JSON the PWA persists (`JSON.stringify` of the normalized context), or null when incomplete. */
fun serializeCashvanMerchantContext(context: CashvanCheckoutNavigationState): String? {
    val normalized = normalizeCashvanMerchantContext(context) ?: return null
    return buildJsonObject {
        put("merchantId", normalized.merchantId)
        put("merchantName", normalized.merchantName)
        put("governorate", normalized.governorate)
        put("priceTier", normalized.priceTier)
    }.toString()
}

/** Prefers the current navigation state; falls back to the stored context (e.g. after a restart). */
fun resolveCashvanMerchantContext(
    navigationState: CashvanCheckoutNavigationState?,
    storedContext: CashvanCheckoutNavigationState?,
): CashvanMerchantContext? =
    normalizeCashvanMerchantContext(navigationState) ?: normalizeCashvanMerchantContext(storedContext)

/** A persisted scroll offset: JS `Number(raw)` when finite and non-negative, else null. */
fun parseCashvanMerchantScrollOffset(raw: String?): Double? {
    if (raw == null) return null
    return jsStringToNumber(raw).takeIf { it.isFinite() && it >= 0 }
}

/** The persisted form of a scroll offset (JS `Math.round`), or null for negative/non-finite offsets. */
fun serializeCashvanMerchantScrollOffset(offset: Double): String? {
    if (!offset.isFinite() || offset < 0) return null
    return jsNumberToString(jsMathRound(offset))
}

fun canRestoreCashvanMerchantScroll(input: CashvanMerchantScrollRestoreInput): Boolean {
    if (input.viewAll) return !input.pagedLoading
    return !input.nearbyLoading && (input.hasQueryLocation || input.hasNearbyError)
}

/**
 * Where checkout must send the agent instead of rendering: nowhere after a
 * successful submit (which clears the cart), merchant selection without a
 * merchant, and back to products (keeping the pricing context) for an empty cart.
 */
fun resolveCheckoutGuardRedirect(input: CheckoutGuardInput): CashvanRouteRedirect? {
    if (input.isSuccess) return null
    val merchantId = input.merchantId
    if (merchantId.isNullOrEmpty()) return CashvanRouteRedirect(CashvanRoute.MERCHANTS)
    if (input.itemCount > 0) return null

    val priceTier = input.priceTier
    val governorate = input.governorate
    if (!priceTier.isNullOrEmpty() && governorate != null) {
        return CashvanRouteRedirect(
            route = CashvanRoute.SELECT_PRODUCTS,
            state = CashvanMerchantContext(
                merchantId = merchantId,
                merchantName = input.merchantName ?: "",
                governorate = governorate.id,
                priceTier = priceTier,
            ),
        )
    }

    return CashvanRouteRedirect(CashvanRoute.MERCHANTS)
}

/** Blank input is a zero rebate; partial decimals like "0." are accepted. */
fun validateRebateInput(rebateInput: String, maxRebate: Double): RebateValidation {
    val normalized = rebateInput.jsTrim()
    val rebateValue = if (normalized.isEmpty()) 0.0 else jsStringToNumber(normalized)

    val errorMessage = when {
        !rebateValue.isFinite() -> REBATE_INVALID_MESSAGE
        rebateValue < 0 -> REBATE_NEGATIVE_MESSAGE
        rebateValue > maxRebate -> REBATE_EXCEEDS_TOTAL_MESSAGE
        else -> null
    }
    return RebateValidation(rebateValue, errorMessage)
}

fun resolveCashvanProductSelectionGuard(
    merchantContext: CashvanMerchantContext?,
    orderCompleted: Boolean,
): CashvanRouteRedirect? = when {
    orderCompleted -> CashvanRouteRedirect(CashvanRoute.HOME)
    merchantContext == null -> CashvanRouteRedirect(CashvanRoute.MERCHANTS)
    else -> null
}

fun resolveCashvanBackNavigation(
    screen: CashvanFlowScreen,
    merchantContext: CashvanMerchantContext?,
): CashvanBackNavigation {
    if (screen == CashvanFlowScreen.CHECKOUT && merchantContext != null) {
        return CashvanBackNavigation(
            route = CashvanRoute.SELECT_PRODUCTS,
            state = merchantContext,
            clearCart = false,
            clearMerchantContext = false,
        )
    }
    return CashvanBackNavigation(route = CashvanRoute.MERCHANTS, clearCart = true, clearMerchantContext = true)
}

/** Back closes an open sheet first (no-order reason, then visit type) before leaving the merchant page. */
fun resolveCashvanMerchantBackAction(
    showNoOrderReason: Boolean,
    showVisitTypeSheet: Boolean,
): CashvanMerchantBackAction = when {
    showNoOrderReason -> CashvanMerchantBackAction.CLOSE_NO_ORDER_REASON
    showVisitTypeSheet -> CashvanMerchantBackAction.CLOSE_VISIT_TYPE
    else -> CashvanMerchantBackAction.NAVIGATE_HOME
}

/** Whether a (browser history) state map already guards [pathname]. */
fun hasCashvanDeviceBackGuard(state: Any?, pathname: String): Boolean =
    (state as? Map<*, *>)?.get(CASHVAN_BACK_GUARD_PATH_KEY) == pathname

/** [state]'s entries plus the guard marker for [pathname]; non-map states start empty. */
fun createCashvanDeviceBackState(state: Any?, pathname: String): Map<String, Any?> {
    val base = (state as? Map<*, *>)?.entries?.associate { (key, value) -> key.toString() to value }.orEmpty()
    return base + (CASHVAN_BACK_GUARD_PATH_KEY to pathname)
}
