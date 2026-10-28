package com.illa.cashvan.sales.domain.cashvan

import com.illa.cashvan.sales.domain.orders.CancellablePresellOrder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

// Ported from the PWA's src/lib/orderSheetActions.test.ts.
class OrderSheetActionsTest {

    private data class Order(override val status: String, override val orderDate: String) : CancellablePresellOrder

    private val orderDate = "2026-07-15T08:00:00.000Z"

    /** One hour after the order — inside the four-hour presell cancel window. */
    private val withinWindow = Instant.parse("2026-07-15T09:00:00.000Z")

    @Test
    fun `lets the agent cancel their own unfulfilled presell order inside the window`() {
        assertEquals(
            OrderSheetActions(showCancel = true, showContact = false),
            getOrderSheetActions(Order("unfulfilled", orderDate), OrderSheetChannel.PRESELL, withinWindow),
        )
    }

    @Test
    fun `never offers cancel on a retail-app order — only the merchant can cancel it`() {
        assertEquals(
            OrderSheetActions(showCancel = false, showContact = false),
            getOrderSheetActions(Order("unfulfilled", orderDate), OrderSheetChannel.RETAIL, withinWindow),
        )
    }

    @Test
    fun `offers call-to-reorder on an expired order in either channel`() {
        OrderSheetChannel.entries.forEach { channel ->
            assertEquals(
                channel.name,
                OrderSheetActions(showCancel = false, showContact = true),
                getOrderSheetActions(Order("expired", orderDate), channel, withinWindow),
            )
        }
    }

    @Test
    fun `offers no actions on a retail order that is fulfilled, in progress or cancelled by the merchant`() {
        listOf("fulfilled", "in_progress", "cancelled_by_merchant").forEach { status ->
            assertEquals(
                status,
                OrderSheetActions(showCancel = false, showContact = false),
                getOrderSheetActions(Order(status, orderDate), OrderSheetChannel.RETAIL, withinWindow),
            )
        }
    }
}
