package com.illa.cashvan.sales.domain.checkout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// DIS-446: truth table for the order-submit feedback catalogue. INSUFFICIENT_STOCK
// was silently dropped once in the PWA — these tests pin EVERY code so a dropped
// branch fails the suite.
class PresellCheckoutErrorsTest {

    // The PWA test used a fake t(); these are the resolved Arabic strings.
    private val tError = "خطأ"
    private val tFailedToSubmit = "مقدرناش نبعت الأوردر"

    private fun shortage(
        productId: String = "product-1",
        sku: String = "SKU-1",
        requested: Int = 10,
        available: Int = 4,
        actualAvailable: Int? = null,
    ) = ShortageLine(productId, sku, requested, available, status = "insufficient", actualAvailable = actualAvailable)

    private fun feedback(code: String, shortages: List<ShortageLine> = emptyList(), reason: String? = null) =
        resolveOrderSubmitFeedback(OrderSubmissionFailure(code, shortages, reason))

    // inventory-race codes refresh balances

    @Test
    fun `inventory-race codes show an Arabic message and request an inventory refresh`() {
        listOf(
            MIXED_INVENTORY_SOURCES_CODE,
            EXPECTED_INVENTORY_UNAVAILABLE_CODE,
            "ACTUAL_STOCK_MUST_BE_CONSUMED_FIRST",
            "INSUFFICIENT_STOCK",
        ).forEach { code ->
            val feedback = feedback(code)
            assertTrue(code, feedback.refreshInventory)
            assertEquals(code, 8000, feedback.durationMs)
            assertNotEquals(code, tError, feedback.title) // specific, not the generic error title
            assertTrue(code, feedback.title.isNotEmpty())
            assertTrue(code, feedback.description.isNotEmpty())
        }
    }

    // non-inventory codes keep balances as-is

    @Test
    fun `non-inventory codes map to their dedicated message without refresh`() {
        listOf(
            "DUPLICATE_VISIT" to "زيارة مسجلة منذ لحظات",
            "INVENTORY_STALE" to "رصيد المخزن غير محدث",
            "INVENTORY_WAREHOUSE_NOT_CONFIGURED" to "لم يتم ربط المحافظة بمخزن",
        ).forEach { (code, title) ->
            val feedback = feedback(code)
            assertEquals(code, title, feedback.title)
            assertFalse(code, feedback.refreshInventory)
            assertEquals(code, 8000, feedback.durationMs)
        }
    }

    @Test
    fun `IDEMPOTENCY_CONFLICT keeps the default toast duration`() {
        val feedback = feedback("IDEMPOTENCY_CONFLICT")
        assertFalse(feedback.refreshInventory)
        assertNull(feedback.durationMs)
        assertEquals(tError, feedback.title)
    }

    // INSUFFICIENT_STOCK shortage detail (restored branch)

    @Test
    fun `lists every shortage as sku, requested and available`() {
        val feedback = feedback(
            "INSUFFICIENT_STOCK",
            listOf(shortage(), shortage(productId = "product-2", sku = "SKU-2", requested = 7, available = 0)),
        )
        assertEquals("الكمية المتاحة غير كافية", feedback.title)
        assertEquals(
            "SKU-1: المطلوب 10 كرتونة، المتاح 4 كرتونة • SKU-2: المطلوب 7 كرتونة، المتاح 0 كرتونة",
            feedback.description,
        )
    }

    @Test
    fun `falls back to a static hint when the RPC returned no shortage rows`() {
        val feedback = feedback("INSUFFICIENT_STOCK")
        assertTrue(feedback.description.isNotEmpty())
        assertTrue(feedback.refreshInventory)
    }

    // ACTUAL_STOCK crossover detail (restored branch)

    @Test
    fun `reports the actual-available balance that must be reserved first`() {
        val feedback = feedback(
            "ACTUAL_STOCK_MUST_BE_CONSUMED_FIRST",
            listOf(shortage(sku = "SKU-9", actualAvailable = 12, available = 12)),
        )
        assertTrue(feedback.description.contains("SKU-9"))
        assertTrue(feedback.description.contains("١٢")) // 12.toLocaleString('ar-EG')
        assertTrue(feedback.description.contains("لا يمكن تقسيم نفس المنتج بين المخزون المتاح والمتوقع"))
    }

    @Test
    fun `falls back to the static message when no shortage rows are attached`() {
        assertEquals(
            "يوجد رصيد فعلي لهذا المنتج ويجب طلبه قبل طلب الكميات المتوقعة.",
            feedback("ACTUAL_STOCK_MUST_BE_CONSUMED_FIRST").description,
        )
    }

    // fallbacks

    @Test
    fun `non-OrderSubmissionError errors get the generic message`() {
        assertEquals(
            OrderSubmitFeedback(title = tError, description = tFailedToSubmit, refreshInventory = false, durationMs = 8000),
            resolveOrderSubmitFeedback(null),
        )
    }

    @Test
    fun `unknown codes surface the backend reason when present`() {
        val feedback = feedback("SOME_NEW_CODE", reason = "رسالة من الخادم")
        assertEquals("رسالة من الخادم", feedback.description)
        assertFalse(feedback.refreshInventory)
    }

    @Test
    fun `unknown codes without a reason use the generic message`() {
        assertEquals(tFailedToSubmit, feedback("SOME_NEW_CODE").description)
        assertEquals(tFailedToSubmit, feedback("SOME_NEW_CODE", reason = "  ").description)
    }
}
