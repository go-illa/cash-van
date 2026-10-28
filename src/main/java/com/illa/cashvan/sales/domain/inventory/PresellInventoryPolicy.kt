package com.illa.cashvan.sales.domain.inventory

import java.time.DateTimeException
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

const val MIXED_INVENTORY_SOURCES_MESSAGE =
    "لا يمكن جمع منتجات متاحة الآن ومنتجات متوقعة في نفس الطلب. أنشئ طلبًا منفصلًا لكل نوع من المخزون."

private fun routingMessage(routingStatus: PresellInventoryRoutingStatus?): String = when (routingStatus) {
    PresellInventoryRoutingStatus.MERCHANT_REGION_MISSING -> "محافظة العميل غير محددة."
    PresellInventoryRoutingStatus.MERCHANT_NOT_FOUND -> "تعذر تحديد بيانات العميل للمخزون."
    else -> "لم يتم ربط محافظة العميل بمخزن نشط."
}

fun getPresellInventoryContext(state: PresellInventoryReadState): PresellInventoryContext {
    val enforcementEnabled = state.config?.enforcementEnabled
    val routingStatus = state.config?.routingStatus

    fun blocked(reason: InventoryBlockReason, message: String) = PresellInventoryContext(
        enforcementEnabled = enforcementEnabled,
        routingStatus = routingStatus,
        controlsReady = false,
        orderBlocked = true,
        blockReason = reason,
        blockMessage = message,
    )

    val ready = PresellInventoryContext(
        enforcementEnabled = enforcementEnabled,
        routingStatus = routingStatus,
        controlsReady = true,
        orderBlocked = false,
        blockReason = null,
        blockMessage = null,
    )

    // Advisory mode must be completely transparent to the agent. Once the
    // server confirms enforcement is disabled, inventory read/routing health
    // no longer gates product controls or checkout.
    if (enforcementEnabled == false) return ready

    if (state.loading) {
        return blocked(InventoryBlockReason.LOADING, "جارٍ تحميل رصيد المخزن. انتظر حتى يكتمل التحقق.")
    }
    if (state.refreshing) {
        return blocked(InventoryBlockReason.REFRESHING, "جارٍ تحديث رصيد المخزن. يمكن خفض الكميات أو حذفها فقط.")
    }
    if (!state.error.isNullOrEmpty()) {
        return blocked(InventoryBlockReason.ERROR, "تعذر التحقق من رصيد المخزن. حدّث الرصيد قبل متابعة الطلب.")
    }
    if (routingStatus != null && routingStatus != PresellInventoryRoutingStatus.CONFIGURED) {
        return blocked(InventoryBlockReason.ROUTING, routingMessage(routingStatus))
    }
    if (enforcementEnabled == null || routingStatus == null) {
        return blocked(InventoryBlockReason.UNKNOWN, "رصيد المخزن غير متاح بعد. حدّث الرصيد قبل متابعة الطلب.")
    }
    if (enforcementEnabled && (state.isStale || state.fetchedAt.isNullOrEmpty())) {
        return blocked(InventoryBlockReason.STALE, "رصيد المخزن قديم ولا يمكن الاعتماد عليه حتى يتم نشر رصيد محدث.")
    }
    return ready
}

private fun wholeUnits(value: Double): Int = max(0, floor(value).toInt())

private fun normalizedAvailableUnits(stock: PresellStockBalance?): Int =
    wholeUnits(stock?.maxReservable ?: stock?.available ?: 0.0)

private fun normalizedActualAvailableUnits(stock: PresellStockBalance?): Int =
    wholeUnits(stock?.actualAvailable ?: stock?.available ?: 0.0)

private fun normalizedExpectedAvailableUnits(stock: PresellStockBalance?): Int =
    wholeUnits(stock?.expectedAvailable ?: 0.0)

// Egyptian Arabic month names as rendered by Intl 'ar-EG' { month: 'long' }.
private val ARABIC_MONTHS = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
)

private val DATE_ONLY = Regex("([0-9]{4})-([0-9]{2})-([0-9]{2})")

private fun toArabicIndicDigits(text: String): String =
    text.map { if (it in '0'..'9') '٠' + (it - '0') else it }.joinToString("")

/**
 * `n.toLocaleString('ar-EG')` for an integer: Arabic-Indic digits grouped with
 * U+066C; ICU prefixes negatives with an Arabic letter mark (U+061C).
 */
internal fun formatArabicEgyptInteger(value: Int): String {
    val grouped = abs(value.toLong()).toString().reversed().chunked(3).joinToString("٬").reversed()
    val digits = toArabicIndicDigits(grouped)
    return if (value < 0) "؜-$digits" else digits
}

/**
 * Formats a backend date-only value like the PWA's
 * `Intl.DateTimeFormat('ar-EG', { day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' })`
 * ("2026-08-14" → "١٤ أغسطس ٢٠٢٦"). The pattern is hardcoded (not the device ICU,
 * which differs between Android versions) to match the PWA's Node/Chrome output.
 * Returns null for absent, malformed or impossible dates.
 */
fun formatExpectedReceiptDate(value: String?): String? {
    val match = value?.let { DATE_ONLY.matchEntire(it) } ?: return null
    val year = match.groupValues[1].toInt()
    val month = match.groupValues[2].toInt()
    val day = match.groupValues[3].toInt()

    // JS Date.UTC maps years 0–99 to 1900–1999, so the PWA's round-trip check rejects them.
    if (year < 100) return null
    val date = try {
        LocalDate.of(year, month, day)
    } catch (_: DateTimeException) {
        return null
    }

    return "${toArabicIndicDigits(date.dayOfMonth.toString())} ${ARABIC_MONTHS[date.monthValue - 1]} " +
        toArabicIndicDigits(date.year.toString())
}

/** Looks the SKU up as-is, then trimmed (catalog SKUs may carry surrounding whitespace). */
fun getPresellStockBalance(stocks: Map<String, PresellStockBalance>, sku: String): PresellStockBalance? =
    stocks[sku] ?: stocks[sku.trim()]

/**
 * DIS-446: items whose SKU has no published stock balance. The RPC drops
 * not_found balances entirely, so absence from [stocks] means the SKU is not
 * found in the warehouse — such items are auto-removed from the cart and the
 * deferred-request basket.
 *
 * Removal is enforcement behaviour: in advisory mode, or while controls are
 * not ready (balances untrustworthy), nothing is ever removed.
 */
fun <T> collectPresellItemsWithoutStockBalance(
    items: List<T>,
    stocks: Map<String, PresellStockBalance>,
    context: PresellInventoryContext,
    skuOf: (T) -> String,
): List<T> {
    if (context.enforcementEnabled != true || !context.controlsReady) return emptyList()
    return items.filter { getPresellStockBalance(stocks, skuOf(it)) == null }
}

/**
 * DIS-446: deferred-request items whose SKU now has actual stock. Enforcement
 * only; "in stock" means genuinely available now — expected-only balances
 * (incoming receipts) do not flip a deferred request.
 */
fun <T> collectPresellRequestItemsInStock(
    items: List<T>,
    stocks: Map<String, PresellStockBalance>,
    context: PresellInventoryContext,
    skuOf: (T) -> String,
): List<T> {
    if (context.enforcementEnabled != true || !context.controlsReady) return emptyList()
    return items.filter { item ->
        val policy = getPresellProductInventoryPolicy(getPresellStockBalance(stocks, skuOf(item)), context)
        policy.kind == ProductInventoryKind.AVAILABLE && policy.actualAvailableUnits > 0
    }
}

/** Available SKUs first; the original order is preserved inside each group (stable sort). */
fun <T> sortPresellProductsByAvailability(
    products: List<T>,
    stocks: Map<String, PresellStockBalance>,
    skuOf: (T) -> String,
): List<T> = products.sortedBy { product ->
    val balance = getPresellStockBalance(stocks, skuOf(product))
    val isAvailable = balance?.status == PresellStockStatus.AVAILABLE && normalizedAvailableUnits(balance) > 0
    if (isAvailable) 0 else 1
}

fun getPresellProductInventoryPolicy(
    stock: PresellStockBalance?,
    context: PresellInventoryContext,
): PresellProductInventoryPolicy {
    if (!context.controlsReady) {
        return PresellProductInventoryPolicy(
            kind = ProductInventoryKind.UNAVAILABLE,
            availableUnits = normalizedAvailableUnits(stock),
            actualAvailableUnits = normalizedActualAvailableUnits(stock),
            expectedAvailableUnits = normalizedExpectedAvailableUnits(stock),
            reservationSource = stock?.recommendedSource ?: ReservationSource.NONE,
            expectedReceiptDate = stock?.expectedReceiptDate,
            canAdd = false,
            maxQuantity = null,
            advisory = false,
            enforcedBlock = false,
            label = context.blockMessage ?: "رصيد المخزن غير متاح حاليًا.",
        )
    }

    val enforced = context.enforcementEnabled == true
    val blockedCap = if (enforced) 0 else null

    if (stock == null) {
        return PresellProductInventoryPolicy(
            kind = ProductInventoryKind.MISSING,
            availableUnits = 0,
            actualAvailableUnits = 0,
            expectedAvailableUnits = 0,
            reservationSource = ReservationSource.NONE,
            expectedReceiptDate = null,
            canAdd = !enforced,
            maxQuantity = blockedCap,
            advisory = false,
            enforcedBlock = enforced,
            label = if (enforced) "لا يوجد رصيد منشور لهذا المنتج — لا يمكن إضافته" else "",
        )
    }

    val availableUnits = normalizedAvailableUnits(stock)
    val actualAvailableUnits = normalizedActualAvailableUnits(stock)
    val expectedAvailableUnits = normalizedExpectedAvailableUnits(stock)
    val reservationSource = stock.recommendedSource ?: ReservationSource.NONE
    val expectedReceiptDate = stock.expectedReceiptDate

    if (stock.status == PresellStockStatus.NOT_FOUND) {
        return PresellProductInventoryPolicy(
            kind = ProductInventoryKind.NOT_FOUND,
            availableUnits = 0,
            actualAvailableUnits = actualAvailableUnits,
            expectedAvailableUnits = expectedAvailableUnits,
            reservationSource = reservationSource,
            expectedReceiptDate = expectedReceiptDate,
            canAdd = !enforced,
            maxQuantity = blockedCap,
            advisory = false,
            enforcedBlock = enforced,
            label = if (enforced) "كود المنتج غير موجود في المخزن — لا يمكن إضافته" else "",
        )
    }

    if (stock.status == PresellStockStatus.OUT_OF_STOCK || availableUnits < 1) {
        return PresellProductInventoryPolicy(
            kind = ProductInventoryKind.OUT_OF_STOCK,
            availableUnits = 0,
            actualAvailableUnits = actualAvailableUnits,
            expectedAvailableUnits = expectedAvailableUnits,
            reservationSource = ReservationSource.NONE,
            expectedReceiptDate = expectedReceiptDate,
            canAdd = !enforced,
            maxQuantity = blockedCap,
            advisory = false,
            enforcedBlock = enforced,
            label = if (enforced) "نفد رصيد المخزن — لا يمكن إضافته" else "",
        )
    }

    val units = formatArabicEgyptInteger(availableUnits)
    val label = when {
        !enforced -> ""
        reservationSource == ReservationSource.EXPECTED -> {
            val datePart = formatExpectedReceiptDate(expectedReceiptDate)?.let { " بتاريخ $it" } ?: ""
            "متوقع$datePart: $units كرتونة"
        }
        else -> "متاح الآن: $units كرتونة"
    }

    return PresellProductInventoryPolicy(
        kind = ProductInventoryKind.AVAILABLE,
        availableUnits = availableUnits,
        actualAvailableUnits = actualAvailableUnits,
        expectedAvailableUnits = expectedAvailableUnits,
        reservationSource = reservationSource,
        expectedReceiptDate = expectedReceiptDate,
        canAdd = true,
        maxQuantity = if (enforced) availableUnits else null,
        advisory = false,
        enforcedBlock = false,
        label = label,
    )
}

fun toPresellInventoryAnalyticsAvailability(reservationSource: ReservationSource): InventoryAnalyticsAvailability =
    when (reservationSource) {
        ReservationSource.ACTUAL -> InventoryAnalyticsAvailability.AVAILABLE
        ReservationSource.EXPECTED -> InventoryAnalyticsAvailability.EXPECTED
        ReservationSource.NONE -> InventoryAnalyticsAvailability.UNKNOWN
    }

private fun issueMessage(policy: PresellProductInventoryPolicy, requested: Int): String {
    if (policy.kind == ProductInventoryKind.NOT_FOUND) return "كود المنتج غير موجود في المخزن"
    if (policy.kind == ProductInventoryKind.MISSING) return "لا يوجد رصيد منشور للمنتج"
    if (policy.availableUnits == 0) return "لا يوجد رصيد متاح"
    if (
        policy.reservationSource == ReservationSource.ACTUAL &&
        policy.expectedAvailableUnits > 0 &&
        requested > policy.actualAvailableUnits
    ) {
        return "يوجد ${formatArabicEgyptInteger(policy.actualAvailableUnits)} كرتونة متاحة فعليًا لهذا المنتج ويجب حجزها أولًا. لا يمكن تقسيم نفس المنتج بين المخزون المتاح والمتوقع."
    }
    return "المطلوب ${formatArabicEgyptInteger(requested)} كرتونة والمتاح ${formatArabicEgyptInteger(policy.availableUnits)} كرتونة"
}

fun assessPresellCartInventory(
    items: List<InventoryCartItemLike>,
    stocks: Map<String, PresellStockBalance>,
    context: PresellInventoryContext,
): PresellCartInventoryAssessment {
    if (context.enforcementEnabled == false) {
        return PresellCartInventoryAssessment(
            blocked = false,
            systemMessage = null,
            blockingIssues = emptyList(),
            advisoryIssues = emptyList(),
        )
    }

    if (context.orderBlocked) {
        return PresellCartInventoryAssessment(
            blocked = items.isNotEmpty(),
            systemMessage = if (items.isNotEmpty()) context.blockMessage else null,
            blockingIssues = emptyList(),
            advisoryIssues = emptyList(),
        )
    }

    val enforced = context.enforcementEnabled == true
    val blockingIssues = mutableListOf<PresellCartInventoryIssue>()
    val reservationSources = mutableSetOf<ReservationSource>()

    items.forEach { item ->
        val policy = getPresellProductInventoryPolicy(getPresellStockBalance(stocks, item.sku), context)
        val exceedsReference = item.quantity > policy.availableUnits
        val shouldBlock = enforced && (policy.enforcedBlock || exceedsReference)

        if (
            enforced &&
            policy.kind == ProductInventoryKind.AVAILABLE &&
            policy.reservationSource != ReservationSource.NONE
        ) {
            reservationSources += policy.reservationSource
        }

        if (shouldBlock) {
            blockingIssues += PresellCartInventoryIssue(
                sku = item.sku,
                description = item.description,
                requested = item.quantity,
                available = policy.availableUnits,
                kind = policy.kind,
                message = issueMessage(policy, item.quantity),
            )
        }
    }

    val mixedSources = reservationSources.size > 1
    return PresellCartInventoryAssessment(
        blocked = blockingIssues.isNotEmpty() || mixedSources,
        systemMessage = if (mixedSources) MIXED_INVENTORY_SOURCES_MESSAGE else null,
        blockingIssues = blockingIssues,
        advisoryIssues = emptyList(),
    )
}

/**
 * DIS-446: whether any cart item currently reserves from EXPECTED stock.
 * Out-of-stock requests may only ride along with actual-available orders or
 * stand alone — never with expected carts. Evaluated against the LIVE
 * balances, so a product that flipped to expected after being added is detected.
 */
fun cartContainsExpectedReservations(
    items: List<InventoryCartItemLike>,
    stocks: Map<String, PresellStockBalance>,
    context: PresellInventoryContext,
): Boolean {
    if (context.enforcementEnabled != true) return false
    return items.any { item ->
        getPresellProductInventoryPolicy(getPresellStockBalance(stocks, item.sku), context)
            .reservationSource == ReservationSource.EXPECTED
    }
}
