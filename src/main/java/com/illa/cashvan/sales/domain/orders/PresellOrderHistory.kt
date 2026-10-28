package com.illa.cashvan.sales.domain.orders

import com.illa.cashvan.sales.domain.format.jsTrim

enum class PresellStatusGroup(val wireValue: String, val label: String) {
    EXPIRED("expired", "منتهي"),
    CANCELED("canceled", "ملغي"),
    FULFILLED("fulfilled", "مكتمل"),
    PARTIALLY_FULFILLED("partially_fulfilled", "مكتمل جزئياً"),
    OUT_FOR_DELIVERY("out_for_delivery", "خرج للتوصيل"),
    ON_HOLD("on_hold", "معلّق"),
    PENDING("pending", "قيد الانتظار"),
    IN_PROGRESS("in_progress", "قيد التنفيذ"),
    UNKNOWN("unknown", "غير معروف"),
}

private val STATUS_GROUPS: Map<String, PresellStatusGroup> = mapOf(
    "expired" to PresellStatusGroup.EXPIRED,
    "canceled" to PresellStatusGroup.CANCELED,
    "cancelled" to PresellStatusGroup.CANCELED,
    "voided" to PresellStatusGroup.CANCELED,
    // Written by the ILLA retail app when a merchant cancels their own app order.
    "cancelled_by_merchant" to PresellStatusGroup.CANCELED,
    "fulfilled" to PresellStatusGroup.FULFILLED,
    "delivered" to PresellStatusGroup.FULFILLED,
    "partially_fulfilled" to PresellStatusGroup.PARTIALLY_FULFILLED,
    "partially_delivered" to PresellStatusGroup.PARTIALLY_FULFILLED,
    "out_for_delivery" to PresellStatusGroup.OUT_FOR_DELIVERY,
    "on_hold" to PresellStatusGroup.ON_HOLD,
    "unfulfilled" to PresellStatusGroup.PENDING,
    "processing" to PresellStatusGroup.IN_PROGRESS,
    "in_progress" to PresellStatusGroup.IN_PROGRESS,
)

/** Unknown future statuses map to [PresellStatusGroup.UNKNOWN], never to "in progress". */
fun getPresellStatusGroup(status: String): PresellStatusGroup = STATUS_GROUPS[status] ?: PresellStatusGroup.UNKNOWN

/** Structural stand-in for the merchant contact fields of an order-history row. */
interface PresellMerchantContact {
    val phoneNumber: String?
    val secondaryPhone: String?
    val phoneInvalid: Boolean
}

private fun normalizeDialablePhone(phone: String?): String? {
    val value = phone?.jsTrim()
    if (value.isNullOrEmpty()) return null

    val hasLeadingPlus = value.startsWith('+')
    // JS /\D/g: everything but ASCII 0-9 is stripped (Arabic-Indic digits included).
    val digits = value.filter { it in '0'..'9' }
    if (digits.isEmpty()) return null

    return if (hasLeadingPlus) "+$digits" else digits
}

/**
 * `tel:` URI for calling the merchant: the primary phone unless it is flagged
 * invalid, else the secondary phone; null when neither is dialable.
 */
fun getPresellMerchantCallHref(merchant: PresellMerchantContact?): String? {
    if (merchant == null) return null

    val primaryPhone = if (merchant.phoneInvalid) null else normalizeDialablePhone(merchant.phoneNumber)
    val phone = primaryPhone ?: normalizeDialablePhone(merchant.secondaryPhone)

    return phone?.let { "tel:$it" }
}
