package com.illa.cashvan.sales.feature.checkout

import com.illa.cashvan.sales.feature.checkout.domain.repository.OrderSubmissionCodes
import com.illa.cashvan.sales.feature.checkout.domain.repository.normalizeOrderBackendError
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderBackendErrorTest {

    @Test
    fun `a recent visit to the same merchant is a duplicate visit`() {
        val error = normalizeOrderBackendError("P0001", "A visit for this merchant already exists within 5 minutes")
        assertEquals(OrderSubmissionCodes.DUPLICATE_VISIT, error.code)
    }

    @Test
    fun `inventory codes inside the message win over the postgres code`() {
        assertEquals(
            OrderSubmissionCodes.MIXED_INVENTORY_SOURCES,
            normalizeOrderBackendError("P0001", "MIXED_INVENTORY_SOURCES_NOT_ALLOWED: actual and expected").code
        )
        assertEquals(
            OrderSubmissionCodes.EXPECTED_INVENTORY_UNAVAILABLE,
            normalizeOrderBackendError(null, "EXPECTED_INVENTORY_DISABLED_OR_STALE").code
        )
    }

    @Test
    fun `other errors keep their code and trimmed message`() {
        val error = normalizeOrderBackendError("23505", "  duplicate key  ")
        assertEquals("23505", error.code)
        assertEquals("duplicate key", error.reason)
        assertEquals(OrderSubmissionCodes.SUBMISSION_FAILED, normalizeOrderBackendError(null, null).code)
    }
}
