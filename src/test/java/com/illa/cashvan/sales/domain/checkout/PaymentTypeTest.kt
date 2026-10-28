package com.illa.cashvan.sales.domain.checkout

import org.junit.Assert.assertEquals
import org.junit.Test

// No TS test exists; these pin the numeric/string asymmetry of the PWA helpers.
class PaymentTypeTest {

    @Test
    fun `labels numeric codes and wire names, falling back to cash`() {
        assertEquals("نقدي", paymentTypeLabel(0))
        assertEquals("آجل", paymentTypeLabel(1))
        assertEquals("فوري", paymentTypeLabel("fawry"))
        assertEquals("آجل", paymentTypeLabel("1"))
        assertEquals("نقدي", paymentTypeLabel(7))
        assertEquals("نقدي", paymentTypeLabel(null as String?))
    }

    @Test
    fun `normalizes only exact codes and wire names`() {
        assertEquals(PaymentType.CREDIT, normalizePaymentType(1))
        assertEquals(PaymentType.FAWRY, normalizePaymentType("fawry"))
        assertEquals(PaymentType.CASH, normalizePaymentType("1"))
        assertEquals(PaymentType.CASH, normalizePaymentType(null as Int?))
    }
}
