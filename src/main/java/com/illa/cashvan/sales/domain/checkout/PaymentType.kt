package com.illa.cashvan.sales.domain.checkout

// Single source of truth for CashVan order payment types.
// Backend enum (distribution-sales app/models/order.rb): cash: 0, credit: 1, fawry: 2.
// The API returns payment_type as a number on order-detail/history endpoints and
// as a string ('cash' | 'credit' | 'fawry') on create — both forms are keyed here.

enum class PaymentType(val wireValue: String, val code: Int) {
    CASH("cash", 0),
    CREDIT("credit", 1),
    FAWRY("fawry", 2),
}

/** Keyed by both the numeric code (as a string, like a JS object key) and the wire name. */
val PAYMENT_TYPE_LABEL: Map<String, String> = mapOf(
    "0" to "نقدي",
    "1" to "آجل",
    "2" to "فوري",
    "cash" to "نقدي",
    "credit" to "آجل",
    "fawry" to "فوري",
)

private val CASH_LABEL = PAYMENT_TYPE_LABEL.getValue("cash")

/** Arabic label; null or unrecognised values fall back to cash. */
fun paymentTypeLabel(value: Int?): String = value?.let { PAYMENT_TYPE_LABEL[it.toString()] } ?: CASH_LABEL

/** Arabic label; null or unrecognised values fall back to cash. Numeric strings ("1") are keys too. */
fun paymentTypeLabel(value: String?): String = value?.let { PAYMENT_TYPE_LABEL[it] } ?: CASH_LABEL

fun normalizePaymentType(value: Int?): PaymentType = when (value) {
    1 -> PaymentType.CREDIT
    2 -> PaymentType.FAWRY
    else -> PaymentType.CASH
}

/**
 * Only the wire names are recognised: the TS uses strict equality, so the
 * numeric string "1" normalizes to cash even though [paymentTypeLabel] maps it to "آجل".
 */
fun normalizePaymentType(value: String?): PaymentType = when (value) {
    "credit" -> PaymentType.CREDIT
    "fawry" -> PaymentType.FAWRY
    else -> PaymentType.CASH
}
