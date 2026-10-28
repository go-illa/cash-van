package com.illa.cashvan.sales.domain.cashvan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

// Kotlin-only: the PWA has no tests for cashvanProductEnrichment.ts or
// fawryMerchantEnrichment.ts; these cover the pure merge halves ported from them.
class CashvanEnrichmentTest {

    private val cola = RailsProduct(id = "product-1", name = "Cola", sku = "SKU-1", imageUrl = null)
    private val chips = RailsProduct(id = "product-2", name = "Chips", sku = "SKU-2", imageUrl = "https://img")

    private fun planProduct(id: String, productId: String, product: RailsProduct? = null) =
        RailsPlanProduct(id = id, productId = productId, product = product)

    private fun orderLine(id: String, planProductId: String, product: RailsProduct? = null) =
        RailsOrderPlanProduct(id = id, planProductId = planProductId, product = product)

    @Test
    fun `maps a Supabase product row with empty-string fallbacks`() {
        assertEquals(
            RailsProduct(id = "p", name = "", sku = "", imageUrl = null),
            SupabaseProductRow(id = "p", name = null, sku = null, imageUrl = null).toRailsProduct(),
        )
    }

    @Test
    fun `backfills only plan products that came back without a product`() {
        val items = listOf(
            planProduct("pp-1", "product-1"),
            planProduct("pp-2", "product-2", product = chips.copy(name = "Rails Chips")),
            planProduct("pp-3", "product-1"),
            planProduct("pp-4", "product-9"),
        )

        assertEquals(listOf("product-1", "product-9"), missingPlanProductProductIds(items))

        val backfilled = backfillPlanProducts(items, mapOf("product-1" to cola, "product-2" to chips))
        assertEquals(listOf(cola, chips.copy(name = "Rails Chips"), cola, null), backfilled.map { it.product })
        assertSame(items, backfillPlanProducts(items, emptyMap()))
    }

    @Test
    fun `backfills order lines through their plan product`() {
        val items = listOf(
            orderLine("1", "pp-1"),
            orderLine("2", "pp-2", product = chips),
            orderLine("3", "pp-3"),
            orderLine("4", "pp-1"),
        )
        val rows = listOf(
            PlanProductProductIdRow(id = "pp-1", productId = "product-1"),
            PlanProductProductIdRow(id = "pp-3", productId = null),
        )

        assertEquals(listOf("pp-1", "pp-3"), missingOrderPlanProductIds(items))
        assertEquals(listOf("product-1"), planProductRowProductIds(rows))

        val backfilled = backfillOrderPlanProducts(items, rows, mapOf("product-1" to cola))
        assertEquals(listOf(cola, chips, null, cola), backfilled.map { it.product })
        assertSame(items, backfillOrderPlanProducts(items, rows, emptyMap()))
    }

    @Test
    fun `copies the Supabase Fawry fields onto matching merchants only`() {
        val merchants = listOf(
            RailsMerchant(id = "m1", name = "One"),
            RailsMerchant(id = "m2", name = "Two", fawryDeviceNumber = "stale", wantsFawryDevice = false),
        )

        val enriched = applyMerchantFawryStatus(
            merchants,
            mapOf("m2" to MerchantFawryStatus(fawryDeviceNumber = null, wantsFawryDevice = true)),
        )

        assertEquals(
            listOf(
                RailsMerchant(id = "m1", name = "One"),
                RailsMerchant(id = "m2", name = "Two", fawryDeviceNumber = null, wantsFawryDevice = true),
            ),
            enriched,
        )
    }
}
