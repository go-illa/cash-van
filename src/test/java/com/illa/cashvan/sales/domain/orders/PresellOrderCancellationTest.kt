package com.illa.cashvan.sales.domain.orders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

// Ported from the PWA's src/pages/OrderHistory.test.ts.
class PresellOrderCancellationTest {

    private data class Order(override val status: String, override val orderDate: String) : CancellablePresellOrder

    private val order = Order(status = "unfulfilled", orderDate = "2026-07-15T08:00:00.000Z")

    @Test
    fun `allows an unfulfilled order before the four-hour boundary`() {
        assertTrue(canCancelPresellOrder(order, Instant.parse("2026-07-15T11:59:59.999Z")))
    }

    @Test
    fun `stops allowing cancellation exactly four hours after creation`() {
        assertFalse(canCancelPresellOrder(order, Instant.parse("2026-07-15T12:00:00.000Z")))
    }

    @Test
    fun `hides cancellation for processing, in_progress, fulfilled and cancelled orders`() {
        listOf("processing", "in_progress", "fulfilled", "cancelled").forEach { status ->
            assertFalse(status, canCancelPresellOrder(order.copy(status = status), Instant.parse("2026-07-15T09:00:00.000Z")))
        }
    }

    @Test
    fun `never allows cancellation for an unparseable order date`() {
        assertFalse(canCancelPresellOrder(order.copy(orderDate = "not-a-date"), Instant.parse("2026-07-15T09:00:00.000Z")))
    }
}
