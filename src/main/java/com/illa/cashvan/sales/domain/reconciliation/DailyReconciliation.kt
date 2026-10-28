package com.illa.cashvan.sales.domain.reconciliation

import com.illa.cashvan.sales.domain.cashvan.CashvanPlanType
import com.illa.cashvan.sales.domain.cashvan.RailsOrder
import com.illa.cashvan.sales.domain.cashvan.RailsPlanProduct
import com.illa.cashvan.sales.domain.cashvan.RawPaymentType
import com.illa.cashvan.sales.domain.cashvan.finiteNumberOrZero
import com.illa.cashvan.sales.domain.checkout.PaymentType
import java.text.Collator
import java.util.Locale
import java.util.concurrent.CancellationException

sealed interface DailyReconciliationPdfExportError {
    data object Cancelled : DailyReconciliationPdfExportError

    data class Failed(val errorType: String) : DailyReconciliationPdfExportError
}

/**
 * Classifies a failed PDF export for analytics. The PWA keys off the browser
 * error's `name` ("AbortError" = Web Share cancelled); here a
 * [CancellationException] (incl. coroutine cancellation) is the cancellation,
 * and a throwable's simple class name is its error type. Fallbacks mirror JS:
 * "Error" for a nameless (anonymous) throwable, otherwise the `typeof` of the
 * value ("string", "number", "boolean", "function", "object" incl. null).
 */
fun classifyDailyReconciliationPdfExportError(error: Any?): DailyReconciliationPdfExportError {
    if (error is CancellationException) return DailyReconciliationPdfExportError.Cancelled

    val errorType = when (error) {
        is Throwable -> error.javaClass.simpleName.ifEmpty { "Error" }
        is CharSequence, is Char -> "string"
        is Number -> "number"
        is Boolean -> "boolean"
        is Function<*> -> "function"
        else -> "object"
    }
    return DailyReconciliationPdfExportError.Failed(errorType)
}

enum class ReconciliationOrderType(val wireValue: String) {
    CASH_VAN("cash_van"),
    PRE_SELL("pre_sell"),
}

private fun describe(value: RawPaymentType?): String = when (value) {
    is RawPaymentType.Code -> value.code.toString()
    is RawPaymentType.Key -> "\"${value.key}\""
    null -> "null"
}

/**
 * Reads `payment_type` in either wire shape: the enum key (case-insensitive)
 * or its integer code. Anything else is treated as cash and reported through
 * [onWarning] (the PWA's console.warn): money must never be silently
 * miscategorized, so an unknown value needs a human to look at it.
 */
fun normalizeReconciliationPaymentType(
    value: RawPaymentType?,
    onWarning: (String) -> Unit = {},
): PaymentType {
    val normalized = when (value) {
        is RawPaymentType.Key -> when (value.key.lowercase(Locale.ROOT)) {
            "cash" -> PaymentType.CASH
            "credit" -> PaymentType.CREDIT
            "fawry" -> PaymentType.FAWRY
            else -> null
        }
        is RawPaymentType.Code -> when (value.code) {
            0 -> PaymentType.CASH
            1 -> PaymentType.CREDIT
            2 -> PaymentType.FAWRY
            else -> null
        }
        null -> null
    }
    if (normalized != null) return normalized

    onWarning("[dailyReconciliation] Unrecognized payment_type ${describe(value)} — treating as cash")
    return PaymentType.CASH
}

fun normalizeOrderType(value: String?, onWarning: (String) -> Unit = {}): ReconciliationOrderType {
    ReconciliationOrderType.entries.firstOrNull { it.wireValue == value }?.let { return it }
    val shown = value?.let { "\"$it\"" } ?: "null"
    onWarning("[dailyReconciliation] Unrecognized order_type $shown — treating as cash_van")
    return ReconciliationOrderType.CASH_VAN
}

private val FULFILLED_PRE_SELL_STATUSES = setOf("fulfilled", "partially_fulfilled")

/**
 * Orders that count toward reconciliation: fulfilled cash_van orders and
 * fulfilled/partially_fulfilled pre_sell orders. Shared by the money totals and
 * the PDF order table so they never disagree about which orders happened.
 */
fun filterReconciledOrders(orders: List<RailsOrder>, onWarning: (String) -> Unit = {}): List<RailsOrder> =
    orders.filter { order ->
        if (normalizeOrderType(order.orderType, onWarning) == ReconciliationOrderType.CASH_VAN) {
            order.status == "fulfilled"
        } else {
            order.status in FULFILLED_PRE_SELL_STATUSES
        }
    }

/**
 * cash_van: the order's own payment type (set by the agent at checkout).
 * pre_sell: the originating sales order's, since the fulfillment order's
 * defaults to cash and is never set; falls back to the order's own when the
 * sales order is missing.
 */
fun resolveOrderPaymentType(order: RailsOrder, onWarning: (String) -> Unit = {}): PaymentType {
    val isCashVan = normalizeOrderType(order.orderType, onWarning) == ReconciliationOrderType.CASH_VAN
    val salesOrder = order.salesOrder
    val source = if (!isCashVan && salesOrder != null) RawPaymentType.Key(salesOrder.paymentType) else order.paymentType
    return normalizeReconciliationPaymentType(source, onWarning)
}

data class DailyReconciliationTotals(
    val cash: Double,
    val credit: Double,
    val fawry: Double,
)

/**
 * Totals per payment type across every reconciled order on the plan,
 * cumulative for the whole plan (no "today" boundary).
 */
fun computeMoneyBreakdown(orders: List<RailsOrder>, onWarning: (String) -> Unit = {}): DailyReconciliationTotals {
    var cash = 0.0
    var credit = 0.0
    var fawry = 0.0

    for (order in filterReconciledOrders(orders, onWarning)) {
        val income = finiteNumberOrZero(order.totalIncome)
        when (resolveOrderPaymentType(order, onWarning)) {
            PaymentType.CASH -> cash += income
            PaymentType.CREDIT -> credit += income
            PaymentType.FAWRY -> fawry += income
        }
    }

    return DailyReconciliationTotals(cash = cash, credit = credit, fawry = fawry)
}

data class PlanProductBreakdownRow(
    val planProductId: String,
    val productId: String,
    val productName: String,
    val sku: String,
    val assignedQuantity: Double,
    val soldQuantity: Double,
    val remainingQuantity: Double,
    val totalValue: Double,
    /** False for a hybrid plan missing either channel sold field: show the aggregates instead. */
    val hasChannelBreakdown: Boolean,
    /** Sold + still available for pre_sell; the two "loaded" values sum to the shared assigned pool. */
    val loadedPreSell: Double,
    val loadedCashVan: Double,
    val soldPreSell: Double,
    val soldCashVan: Double,
    val availablePreSell: Double,
    val availableCashVan: Double,
)

/**
 * Per-SKU breakdown of what's still in the vehicle vs. reserved for pre_sell.
 * The aggregate assigned/sold/available quantities are authoritative. A
 * single-channel plan attributes them to its only channel; a hybrid plan (or an
 * unknown [planType]) needs both channel sold fields, else
 * [PlanProductBreakdownRow.hasChannelBreakdown] is false and the channel values are 0.
 */
fun computePlanProductBreakdown(
    planProducts: List<RailsPlanProduct>,
    planType: CashvanPlanType?,
): List<PlanProductBreakdownRow> = planProducts.map { planProduct ->
    val assignedQuantity = finiteNumberOrZero(planProduct.assignedQuantity)
    val soldQuantity = finiteNumberOrZero(planProduct.soldQuantity)
    val remainingQuantity = finiteNumberOrZero(planProduct.totalAvailableQuantity)

    val hasChannelBreakdown: Boolean
    val soldPreSell: Double
    val soldCashVan: Double
    val availablePreSell: Double
    val availableCashVan: Double

    when (planType) {
        CashvanPlanType.CASH_VAN -> {
            hasChannelBreakdown = true
            soldPreSell = 0.0
            availablePreSell = 0.0
            soldCashVan = soldQuantity
            availableCashVan = remainingQuantity
        }
        CashvanPlanType.PRE_SELL -> {
            hasChannelBreakdown = true
            soldPreSell = soldQuantity
            availablePreSell = remainingQuantity
            soldCashVan = 0.0
            availableCashVan = 0.0
        }
        else -> {
            hasChannelBreakdown = planProduct.preSellSoldQuantity?.isFinite() == true &&
                planProduct.cashVanSoldQuantity?.isFinite() == true
            if (hasChannelBreakdown) {
                soldPreSell = finiteNumberOrZero(planProduct.preSellSoldQuantity)
                soldCashVan = finiteNumberOrZero(planProduct.cashVanSoldQuantity)
                availablePreSell = finiteNumberOrZero(planProduct.preSellAvailableQuantity)
                availableCashVan = finiteNumberOrZero(planProduct.cashVanAvailableQuantity)
            } else {
                soldPreSell = 0.0
                soldCashVan = 0.0
                availablePreSell = 0.0
                availableCashVan = 0.0
            }
        }
    }

    PlanProductBreakdownRow(
        planProductId = planProduct.id,
        productId = planProduct.productId,
        productName = planProduct.product?.name ?: "",
        sku = planProduct.product?.sku ?: "",
        assignedQuantity = assignedQuantity,
        soldQuantity = soldQuantity,
        remainingQuantity = remainingQuantity,
        totalValue = finiteNumberOrZero(planProduct.totalIncome),
        hasChannelBreakdown = hasChannelBreakdown,
        loadedPreSell = soldPreSell + availablePreSell,
        loadedCashVan = soldCashVan + availableCashVan,
        soldPreSell = soldPreSell,
        soldCashVan = soldCashVan,
        availablePreSell = availablePreSell,
        availableCashVan = availableCashVan,
    )
}

const val OTHER_PRINCIPAL_LABEL = "منتجات أخرى"

data class PrincipalGroup(
    val principalName: String,
    val rows: List<PlanProductBreakdownRow>,
)

/**
 * Groups SKU rows by principal (brand/vendor) name, sorted with the Arabic
 * collator (`localeCompare(b, 'ar')`), with unmatched SKUs last under
 * [OTHER_PRINCIPAL_LABEL]. Shared by the on-screen cards and the PDF.
 */
fun groupRowsByPrincipal(
    rows: List<PlanProductBreakdownRow>,
    principalBySku: Map<String, String>?,
): List<PrincipalGroup> {
    val groups = LinkedHashMap<String, MutableList<PlanProductBreakdownRow>>()
    for (row in rows) {
        val principalName = row.sku.takeIf { it.isNotEmpty() }
            ?.let { principalBySku?.get(it) }
            ?.takeIf { it.isNotEmpty() }
            ?: OTHER_PRINCIPAL_LABEL
        groups.getOrPut(principalName) { mutableListOf() } += row
    }

    val collator = Collator.getInstance(Locale.forLanguageTag("ar"))
    return groups.entries
        .sortedWith { (a, _), (b, _) ->
            when {
                a == b -> 0
                a == OTHER_PRINCIPAL_LABEL -> 1
                b == OTHER_PRINCIPAL_LABEL -> -1
                else -> collator.compare(a, b)
            }
        }
        .map { (principalName, groupRows) -> PrincipalGroup(principalName, groupRows.toList()) }
}
