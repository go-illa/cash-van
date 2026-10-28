package com.illa.cashvan.sales.domain.cashvan

import com.illa.cashvan.sales.domain.format.formatNumber
import com.illa.cashvan.sales.domain.format.parseJsDateMillis
import java.time.Instant
import java.time.ZoneId

private const val LINE = "--------------------------------"
private const val WIDE = "================================"
private const val RECEIPT_WIDTH = 32

/** Keyed like the PWA's JS object, so the numeric code 2 and unknown keys fall back to "Cash". */
private val RECEIPT_PAYMENT_LABEL = mapOf(
    "0" to "Cash / نقدي",
    "1" to "Credit / آجل",
    "cash" to "Cash / نقدي",
    "credit" to "Credit / آجل",
    "fawry" to "Fawry",
)

data class ReceiptAgent(val name: String, val code: String)

/** `toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })`. */
private fun formatAmount(value: Double?): String {
    val number = value ?: 0.0
    val formatted = formatNumber(number, 2)
    if (!number.isFinite()) return formatted
    return when (formatted.substringAfter('.', "").length) {
        0 -> "$formatted.00"
        1 -> "${formatted}0"
        else -> formatted
    }
}

/** `toLocaleString('en-GB', { year: 'numeric', month/day/hour/minute: '2-digit' })` → "28/07/2026, 11:05". */
private fun formatReceiptDate(iso: String, zone: ZoneId): String {
    val millis = parseJsDateMillis(iso, zone) ?: return "Invalid Date"
    val time = Instant.ofEpochMilli(millis).atZone(zone)
    fun twoDigits(value: Int) = value.toString().padStart(2, '0')
    return "${twoDigits(time.dayOfMonth)}/${twoDigits(time.monthValue)}/${time.year}, " +
        "${twoDigits(time.hour)}:${twoDigits(time.minute)}"
}

private fun padLine(label: String, value: String): String {
    val gap = RECEIPT_WIDTH - label.length - value.length
    return label + " ".repeat(maxOf(1, gap)) + value
}

/**
 * The 32-column plain-text receipt sent to the thermal printer. [zone] is the
 * device time zone the PWA formats the order date in.
 *
 * Amounts are numbers here; if Rails sends them as strings the PWA would
 * string-concatenate the subtotal, which is not reproduced.
 */
fun buildReceiptText(
    order: RailsOrder,
    agent: ReceiptAgent,
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val merchant = order.merchant
    val items = order.orderPlanProducts.orEmpty()

    val subtotal = items.fold(0.0) { sum, item -> sum + (item.totalIncome ?: 0.0) }
    val rebate = order.rebateValue ?: 0.0
    val total = maxOf(0.0, (order.totalIncome ?: subtotal) - rebate)
    val totalVat = items.fold(0.0) { sum, item -> sum + (item.totalPriceDetails?.total?.vatAmount ?: 0.0) }
    val totalDiscount = items.fold(0.0) { sum, item ->
        val totals = item.totalPriceDetails?.total
        sum + (if (totals != null) totals.discountAmount + totals.cashDiscountAmount else 0.0)
    }

    val paymentKey = when (val paymentType = order.paymentType) {
        is RawPaymentType.Code -> paymentType.code.toString()
        is RawPaymentType.Key -> paymentType.key
        null -> null
    }

    val lines = mutableListOf(
        WIDE,
        "   Front Door Logistic",
        "  فرونت دوور لوجستيك",
        WIDE,
        padLine("Invoice:", order.formattedCode),
        padLine("Date:", formatReceiptDate(order.createdAt, zone)),
        padLine("Agent:", agent.name.ifEmpty { agent.code }),
        padLine("Payment:", paymentKey?.let { RECEIPT_PAYMENT_LABEL[it] } ?: "Cash"),
        LINE,
    )

    if (merchant != null) {
        lines += "Client: ${merchant.signName?.ifEmpty { null } ?: merchant.name}"
        if (!merchant.phoneNumber.isNullOrEmpty()) lines += "Phone: ${merchant.phoneNumber}"
        lines += WIDE
    }

    lines += "Item                Qty   Total"
    lines += LINE

    for (item in items) {
        val quantity = (item.soldQuantity ?: item.quantitySold ?: item.quantity)?.toString() ?: "undefined"
        val name = (item.product?.name ?: "#${item.planProductId}").take(18)
        lines += "${name.padEnd(18)} ${quantity.padStart(3)} ${formatAmount(item.totalIncome).padStart(8)}"
    }

    lines += LINE
    lines += padLine("Subtotal:", "${formatAmount(subtotal)} EGP")
    if (totalVat > 0) lines += padLine("VAT:", "${formatAmount(totalVat)} EGP")
    if (totalDiscount > 0) lines += padLine("Discount:", "-${formatAmount(totalDiscount)} EGP")
    if (rebate > 0) lines += padLine("Rebate:", "-${formatAmount(rebate)} EGP")
    lines += WIDE
    lines += padLine("TOTAL:", "${formatAmount(total)} EGP")
    lines += WIDE
    lines += ""
    lines += ""

    return lines.joinToString("\n")
}
