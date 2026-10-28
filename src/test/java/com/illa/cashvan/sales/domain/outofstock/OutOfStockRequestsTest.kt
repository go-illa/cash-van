package com.illa.cashvan.sales.domain.outofstock

import org.junit.Assert.assertEquals
import org.junit.Test

// DIS-446: the payload-preparation and server-status-mapping halves of the
// PWA's submitOutOfStockRequest. The RPC call itself lives in the repository.
class OutOfStockRequestsTest {

    private fun item(productId: String, quantity: Int) = OutOfStockRequestItemInput(productId, quantity)

    private val baseItems = listOf(item("product-1", 3), item("product-2", 5))

    // item validation (client fast path)

    @Test
    fun `rejects when no item is valid`() {
        // The TS case also includes quantity 1.5, which an Int quantity cannot express.
        assertEquals(emptyList<OutOfStockRequestItemInput>(), collectValidOutOfStockItems(listOf(item("product-1", 0), item("product-2", -2))))
    }

    @Test
    fun `an empty basket never reaches the RPC`() {
        assertEquals(emptyList<OutOfStockRequestItemInput>(), collectValidOutOfStockItems(emptyList()))
    }

    @Test
    fun `drops invalid quantities and collapses duplicate products - last entry wins, like the RPC`() {
        val items = listOf(
            item("product-1", 2),
            item("product-1", 4), // duplicate product_id — LAST entry wins
            item("product-2", 0), // non-positive — dropped
            item("product-3", 7),
        )

        assertEquals(listOf(item("product-1", 4), item("product-3", 7)), collectValidOutOfStockItems(items))
    }

    // RPC call shape

    @Test
    fun `sends the raw basket unchanged when every line is valid`() {
        assertEquals(baseItems, collectValidOutOfStockItems(baseItems))
    }

    @Test
    fun `a replay with edited quantities sends the FULL new payload`() {
        assertEquals(listOf(item("product-1", 9)), collectValidOutOfStockItems(listOf(item("product-1", 9))))
        assertEquals(
            SubmitOutOfStockRequestResult.IdempotentReplay,
            resolveOutOfStockRpcResponse(errorMessage = null, hasData = true, status = "idempotent_replay"),
        )
    }

    // server status mapping

    @Test
    fun `passes submitted, idempotent_replay and routing_failed through untouched`() {
        assertEquals(SubmitOutOfStockRequestResult.Submitted, mapOutOfStockRpcStatus("submitted"))
        assertEquals(SubmitOutOfStockRequestResult.IdempotentReplay, mapOutOfStockRpcStatus("idempotent_replay"))
        assertEquals(SubmitOutOfStockRequestResult.RoutingFailed, mapOutOfStockRpcStatus("routing_failed"))
    }

    @Test
    fun `maps no_valid_items to a failed result with the Arabic message`() {
        assertEquals(
            SubmitOutOfStockRequestResult.Failed("لا توجد منتجات صالحة في الطلب المؤجل"),
            mapOutOfStockRpcStatus("no_valid_items"),
        )
    }

    @Test
    fun `maps forbidden to a failed result with a generic Arabic message`() {
        assertEquals(
            SubmitOutOfStockRequestResult.Failed("تعذر إرسال الطلب المؤجل، حاول مرة أخرى"),
            mapOutOfStockRpcStatus("forbidden"),
        )
    }

    @Test
    fun `maps terminal_status to its own non-retryable variant - never degrades to failed`() {
        assertEquals(SubmitOutOfStockRequestResult.TerminalStatus, mapOutOfStockRpcStatus("terminal_status"))
    }

    @Test
    fun `maps an unknown status defensively to a failed result`() {
        assertEquals(
            SubmitOutOfStockRequestResult.Failed("تعذر إرسال الطلب المؤجل، حاول مرة أخرى"),
            mapOutOfStockRpcStatus("something_new"),
        )
    }

    // RPC errors

    @Test
    fun `fails with the RPC error message`() {
        assertEquals(
            SubmitOutOfStockRequestResult.Failed("function does not exist"),
            resolveOutOfStockRpcResponse(errorMessage = "function does not exist", hasData = false, status = null),
        )
    }

    @Test
    fun `fails defensively when the RPC resolves with neither data nor error`() {
        assertEquals(
            SubmitOutOfStockRequestResult.Failed("تعذر إرسال الطلب المؤجل، حاول مرة أخرى"),
            resolveOutOfStockRpcResponse(errorMessage = null, hasData = false, status = null),
        )
    }
}
