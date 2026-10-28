package com.illa.cashvan.sales.domain.orders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PresellOrderHistoryTest {

    private data class Contact(
        override val phoneNumber: String?,
        override val secondaryPhone: String?,
        override val phoneInvalid: Boolean,
    ) : PresellMerchantContact

    @Test
    fun `maps every backend status to its presentation group and label`() {
        listOf(
            Triple("expired", PresellStatusGroup.EXPIRED, "منتهي"),
            Triple("canceled", PresellStatusGroup.CANCELED, "ملغي"),
            Triple("cancelled", PresellStatusGroup.CANCELED, "ملغي"),
            Triple("voided", PresellStatusGroup.CANCELED, "ملغي"),
            Triple("cancelled_by_merchant", PresellStatusGroup.CANCELED, "ملغي"),
            Triple("fulfilled", PresellStatusGroup.FULFILLED, "مكتمل"),
            Triple("delivered", PresellStatusGroup.FULFILLED, "مكتمل"),
            Triple("partially_fulfilled", PresellStatusGroup.PARTIALLY_FULFILLED, "مكتمل جزئياً"),
            Triple("partially_delivered", PresellStatusGroup.PARTIALLY_FULFILLED, "مكتمل جزئياً"),
            Triple("out_for_delivery", PresellStatusGroup.OUT_FOR_DELIVERY, "خرج للتوصيل"),
            Triple("on_hold", PresellStatusGroup.ON_HOLD, "معلّق"),
            Triple("unfulfilled", PresellStatusGroup.PENDING, "قيد الانتظار"),
            Triple("processing", PresellStatusGroup.IN_PROGRESS, "قيد التنفيذ"),
            Triple("in_progress", PresellStatusGroup.IN_PROGRESS, "قيد التنفيذ"),
        ).forEach { (status, expectedGroup, expectedLabel) ->
            val group = getPresellStatusGroup(status)
            assertEquals(status, expectedGroup, group)
            assertEquals(status, expectedLabel, group.label)
        }
    }

    @Test
    fun `does not misrepresent an unknown future status as in progress`() {
        val group = getPresellStatusGroup("future_status")
        assertEquals(PresellStatusGroup.UNKNOWN, group)
        assertEquals("غير معروف", group.label)
    }

    @Test
    fun `uses and normalizes the valid primary phone`() {
        assertEquals(
            "tel:+201001234567",
            getPresellMerchantCallHref(Contact("+20 100-123-4567", "01111111111", phoneInvalid = false)),
        )
    }

    @Test
    fun `falls back to the secondary phone when the primary is flagged invalid`() {
        assertEquals(
            "tel:01112223333",
            getPresellMerchantCallHref(Contact("01001234567", "0111 222 3333", phoneInvalid = true)),
        )
    }

    @Test
    fun `returns no call target when neither phone can be used`() {
        assertNull(getPresellMerchantCallHref(Contact(null, "---", phoneInvalid = false)))
        assertNull(getPresellMerchantCallHref(null))
    }
}
