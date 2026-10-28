package com.illa.cashvan.sales.domain.checkout

import com.illa.cashvan.sales.domain.format.jsTrim
import com.illa.cashvan.sales.domain.inventory.formatArabicEgyptInteger

// DIS-446: error → user-feedback mapping for the presell order submit path.
// Every code the place_presell_order_with_inventory RPC can reject with (plus
// the client-side normalization codes) maps to a toast title, description, and
// whether the on-screen inventory balances must be refreshed.

const val MIXED_INVENTORY_SOURCES_CODE = "MIXED_INVENTORY_SOURCES_NOT_ALLOWED"
const val EXPECTED_INVENTORY_UNAVAILABLE_CODE = "EXPECTED_INVENTORY_DISABLED_OR_STALE"
const val ACTUAL_STOCK_MUST_BE_CONSUMED_FIRST_CODE = "ACTUAL_STOCK_MUST_BE_CONSUMED_FIRST"
const val INSUFFICIENT_STOCK_CODE = "INSUFFICIENT_STOCK"
const val DUPLICATE_VISIT_CODE = "DUPLICATE_VISIT"
const val INVENTORY_STALE_CODE = "INVENTORY_STALE"
const val INVENTORY_WAREHOUSE_NOT_CONFIGURED_CODE = "INVENTORY_WAREHOUSE_NOT_CONFIGURED"
const val IDEMPOTENCY_CONFLICT_CODE = "IDEMPOTENCY_CONFLICT"

/** A per-SKU shortage row attached to an inventory rejection (PWA `InventoryShortage`). */
data class ShortageLine(
    val productId: String,
    val sku: String,
    val requested: Int,
    val available: Int,
    val status: String,
    /** Actual (non-expected) balance; the crossover message falls back to [available]. */
    val actualAvailable: Int? = null,
)

/** The PWA's `OrderSubmissionError`: a normalized backend/client error code plus detail. */
data class OrderSubmissionFailure(
    val code: String,
    val shortages: List<ShortageLine> = emptyList(),
    val reason: String? = null,
)

data class OrderSubmitFeedback(
    val title: String,
    val description: String,
    /** True when the page must re-fetch stock balances (on-screen numbers are stale). */
    val refreshInventory: Boolean,
    /** Toast duration; null keeps the default. */
    val durationMs: Int?,
)

// i18n keys resolved from public/locales/ar/translation.json.
private const val T_ERROR = "خطأ" // t('error')
private const val T_FAILED_TO_SUBMIT_SALES_ORDER = "مقدرناش نبعت الأوردر" // t('Failed to submit sales order')

private const val LONG_TOAST_MS = 8000

/** `${sku}: المطلوب X كرتونة، المتاح Y كرتونة` per shortage (Western digits), joined for a toast body. */
private fun describeShortages(shortages: List<ShortageLine>): String = shortages.joinToString(" • ") {
    "${it.sku}: المطلوب ${it.requested} كرتونة، المتاح ${it.available} كرتونة"
}

/** Actual-balance crossover detail (Arabic-Indic digits, as the PWA's `toLocaleString('ar-EG')`). */
private fun describeCrossover(shortages: List<ShortageLine>): String = shortages.joinToString(" • ") {
    "${it.sku}: يوجد ${formatArabicEgyptInteger(it.actualAvailable ?: it.available)} كرتونة متاحة فعليًا ويجب حجزها أولًا"
}

/**
 * Maps a failed order submission to its toast. Pass null for an unexpected
 * error that is not an [OrderSubmissionFailure] (the PWA's non-OrderSubmissionError branch).
 */
fun resolveOrderSubmitFeedback(failure: OrderSubmissionFailure?): OrderSubmitFeedback {
    if (failure == null) {
        return OrderSubmitFeedback(T_ERROR, T_FAILED_TO_SUBMIT_SALES_ORDER, refreshInventory = false, durationMs = LONG_TOAST_MS)
    }

    return when (failure.code) {
        MIXED_INVENTORY_SOURCES_CODE -> OrderSubmitFeedback(
            title = "يجب فصل الطلبات حسب نوع المخزون",
            description = "لا يمكن جمع منتجات متاحة الآن ومنتجات متوقعة في نفس الطلب. أنشئ طلبًا منفصلًا لكل نوع.",
            refreshInventory = true,
            durationMs = LONG_TOAST_MS,
        )
        EXPECTED_INVENTORY_UNAVAILABLE_CODE -> OrderSubmitFeedback(
            title = "المخزون المتوقع غير متاح الآن",
            description = "تم إيقاف الحجز من المخزون المتوقع أو تعذر تحديثه. حدّث المخزون وحاول بكمية متاحة فعليًا.",
            refreshInventory = true,
            durationMs = LONG_TOAST_MS,
        )
        ACTUAL_STOCK_MUST_BE_CONSUMED_FIRST_CODE -> {
            val crossoverText = describeCrossover(failure.shortages)
            OrderSubmitFeedback(
                title = "يجب استهلاك المخزون الفعلي أولًا",
                description = if (crossoverText.isNotEmpty()) {
                    "$crossoverText. لا يمكن تقسيم نفس المنتج بين المخزون المتاح والمتوقع."
                } else {
                    "يوجد رصيد فعلي لهذا المنتج ويجب طلبه قبل طلب الكميات المتوقعة."
                },
                refreshInventory = true,
                durationMs = LONG_TOAST_MS,
            )
        }
        INSUFFICIENT_STOCK_CODE -> OrderSubmitFeedback(
            title = "الكمية المتاحة غير كافية",
            description = describeShortages(failure.shortages).ifEmpty {
                "رصيد المخزن غير كافٍ لكميات الطلب. حدّث المخزون وعدّل الكميات ثم أعد المحاولة."
            },
            refreshInventory = true,
            durationMs = LONG_TOAST_MS,
        )
        DUPLICATE_VISIT_CODE -> OrderSubmitFeedback(
            title = "زيارة مسجلة منذ لحظات",
            description = "تم تسجيل زيارة لهذا العميل خلال الدقائق الأخيرة. انتظر قليلًا ثم أعد المحاولة.",
            refreshInventory = false,
            durationMs = LONG_TOAST_MS,
        )
        INVENTORY_STALE_CODE -> OrderSubmitFeedback(
            title = "رصيد المخزن غير محدث",
            description = "لا يمكن تسجيل الطلب حتى يتم تحديث رصيد المخزن أو تعطيل التحقق من الإعدادات.",
            refreshInventory = false,
            durationMs = LONG_TOAST_MS,
        )
        INVENTORY_WAREHOUSE_NOT_CONFIGURED_CODE -> OrderSubmitFeedback(
            title = "لم يتم ربط المحافظة بمخزن",
            description = "لا يمكن تسجيل الطلب قبل ربط محافظة العميل بمخزن نشط في إعدادات المخزون.",
            refreshInventory = false,
            durationMs = LONG_TOAST_MS,
        )
        IDEMPOTENCY_CONFLICT_CODE -> OrderSubmitFeedback(
            title = T_ERROR,
            description = "تم استخدام رقم المحاولة لطلب مختلف. أعد فتح سلة العميل وحاول مرة أخرى.",
            refreshInventory = false,
            durationMs = null,
        )
        // describeOrderSubmissionError: the backend reason when present (DUPLICATE_VISIT
        // never reaches here, so its special message is unreachable in this path).
        else -> OrderSubmitFeedback(
            title = T_ERROR,
            description = failure.reason?.jsTrim()?.ifEmpty { null } ?: T_FAILED_TO_SUBMIT_SALES_ORDER,
            refreshInventory = false,
            durationMs = LONG_TOAST_MS,
        )
    }
}
