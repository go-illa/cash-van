package com.illa.cashvan.sales.domain.nearby

// DIS-316 — WhatsApp reassignment request helpers, ported from the PWA's
// src/lib/whatsapp.ts. The destination number comes at runtime from
// `public.settings` (key `reassignment_whatsapp_number`); not ported.

private const val REASSIGNMENT_REQUESTS_ADMIN_URL = "https://admin.illatrucking.com/routes/reassignment-requests"

private val NON_DIGITS = Regex("[^0-9]")
private val EGYPTIAN_LOCAL_MOBILE = Regex("01[0-9]{9}")

/** How to open the link: in place (native app link) or in a reserved popup (web link). */
enum class WhatsAppLaunchTarget { CURRENT, POPUP }

data class WhatsAppLaunch(val link: String, val target: WhatsAppLaunchTarget)

/** Direct addition of an unassigned merchant (already approved). */
data class DirectApprovalMessageParams(
    val agentName: String,
    val agentPhone: String,
    val merchantName: String,
    val merchantCode: String? = null,
    val toRouteName: String,
)

/** Pending request to move a merchant from another route. */
data class ReassignmentMessageParams(
    val agentName: String,
    val agentPhone: String,
    val merchantName: String,
    val merchantCode: String? = null,
    val fromRouteName: String,
    val toRouteName: String,
)

/** "label: value", omitted for a null or empty value. */
private fun optionalLine(label: String, value: String?): List<String> =
    if (value.isNullOrEmpty()) emptyList() else listOf("$label: $value")

/** Informational Arabic notification: the merchant was already added to the agent's route. */
fun buildDirectApprovalMessage(p: DirectApprovalMessageParams): String = buildList {
    add("إشعار إضافة تاجر غير مُعيّن إلى مسار")
    add("")
    add("نوع العملية: إضافة مباشرة - تمت الموافقة تلقائيًا")
    add("المندوب: ${p.agentName}")
    addAll(optionalLine("رقم المندوب", p.agentPhone))
    add("التاجر: ${p.merchantName}")
    addAll(optionalLine("كود التاجر", p.merchantCode))
    add("من: غير مُعيّن")
    add("إلى: ${p.toRouteName}")
    add("")
    add("الحالة: تم إضافة التاجر على النظام ولا يحتاج موافقة إضافية.")
}.joinToString("\n")

/** Arabic approval request for a merchant on another route. */
fun buildReassignmentMessage(p: ReassignmentMessageParams): String = buildList {
    add("طلب نقل تاجر بين المسارات")
    add("")
    add("نوع العملية: طلب موافقة على نقل تاجر مُعيّن لمسار آخر")
    add("المندوب: ${p.agentName}")
    addAll(optionalLine("رقم المندوب", p.agentPhone))
    add("التاجر: ${p.merchantName}")
    addAll(optionalLine("كود التاجر", p.merchantCode))
    add("المسار الحالي: ${p.fromRouteName}")
    add("المسار المطلوب: ${p.toRouteName}")
    add("")
    add("الحالة: قيد المراجعة.")
    add("الإجراء المطلوب: مراجعة الطلب والموافقة أو الرفض من لوحة الإدارة.")
    add("رابط لوحة الإدارة: $REASSIGNMENT_REQUESTS_ADMIN_URL")
}.joinToString("\n")

/** Digits only; an Egyptian local mobile (01XXXXXXXXX) gets the 2 country prefix. */
private fun normalizeWhatsAppNumber(rawNumber: String?): String {
    val digits = rawNumber.orEmpty().replace(NON_DIGITS, "")
    return if (EGYPTIAN_LOCAL_MOBILE.matches(digits)) "2$digits" else digits
}

/** wa.me link with the message pre-filled, or null when the number has no digits. */
fun buildReassignmentWaLink(message: String, rawNumber: String?): String? {
    val number = normalizeWhatsAppNumber(rawNumber).ifEmpty { return null }
    return "https://wa.me/$number?text=${encodeURIComponent(message)}"
}

/** whatsapp:// deep link with the message pre-filled, or null when the number has no digits. */
fun buildNativeWhatsAppLink(message: String, rawNumber: String?): String? {
    val number = normalizeWhatsAppNumber(rawNumber).ifEmpty { return null }
    return "whatsapp://send?phone=$number&text=${encodeURIComponent(message)}"
}

/**
 * The link to open and how. The PWA picks the native link on Android/iOS user
 * agents and wa.me on desktop; the native app always runs on Android, hence the default.
 */
fun buildWhatsAppLaunch(message: String, rawNumber: String?, useNativeLink: Boolean = true): WhatsAppLaunch? {
    val link = if (useNativeLink) {
        buildNativeWhatsAppLink(message, rawNumber)
    } else {
        buildReassignmentWaLink(message, rawNumber)
    } ?: return null
    return WhatsAppLaunch(link, if (useNativeLink) WhatsAppLaunchTarget.CURRENT else WhatsAppLaunchTarget.POPUP)
}

private const val URI_UNRESERVED_MARKS = "-_.!~*'()"
private const val HEX_DIGITS = "0123456789ABCDEF"

/**
 * JS `encodeURIComponent`: UTF-8 percent-encoding of everything except
 * `A–Z a–z 0–9 - _ . ! ~ * ' ( )`. Like JS, a lone surrogate throws.
 */
internal fun encodeURIComponent(value: String): String = buildString {
    var index = 0
    while (index < value.length) {
        val char = value[index]
        if (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in URI_UNRESERVED_MARKS) {
            append(char)
            index++
            continue
        }
        val codePoint = value.codePointAt(index)
        require(!Character.isSurrogate(char) || Character.isSupplementaryCodePoint(codePoint)) { "URI malformed" }
        for (byte in String(Character.toChars(codePoint)).toByteArray(Charsets.UTF_8)) {
            val unsigned = byte.toInt() and 0xFF
            append('%').append(HEX_DIGITS[unsigned shr 4]).append(HEX_DIGITS[unsigned and 0x0F])
        }
        index += Character.charCount(codePoint)
    }
}
