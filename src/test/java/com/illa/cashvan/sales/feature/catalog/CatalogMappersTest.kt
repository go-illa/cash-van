package com.illa.cashvan.sales.feature.catalog

import com.illa.cashvan.sales.domain.cart.PriceTier
import com.illa.cashvan.sales.domain.inventory.PresellInventoryRoutingStatus
import com.illa.cashvan.sales.domain.inventory.PresellStockStatus
import com.illa.cashvan.sales.domain.inventory.ReservationSource
import com.illa.cashvan.sales.feature.catalog.data.mapper.toInventorySnapshot
import com.illa.cashvan.sales.feature.catalog.data.mapper.toPriceInfo
import com.illa.cashvan.sales.feature.catalog.data.mapper.toStoredProduct
import com.illa.cashvan.sales.feature.catalog.data.model.PricedProductRow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogMappersTest {

    private fun obj(text: String) = Json.parseToJsonElement(text).jsonObject

    @Test
    fun `price rows map with defaults for missing numbers`() {
        val info = obj("""{"id":"p1","product_id":"x","price_tier":"wholesale","final_price":95.5,"cash_discount":null,"discount_details":{"a":1}}""")
            .toPriceInfo()
        assertEquals(PriceTier.WHOLESALE, info.priceTier)
        assertEquals(95.5, info.finalPrice!!, 0.0)
        assertEquals(0.0, info.price, 0.0)
        assertNull(info.cashDiscount)
        assertEquals("""{"a":1}""", info.discountDetails)
    }

    @Test
    fun `stored price is the final price minus the cash discount`() {
        val row = PricedProductRow(
            id = "x",
            sku = "SKU1",
            description = "Tea",
            product_prices = listOf(obj("""{"id":"p1","final_price":100,"cash_discount":2.5}"""))
        )
        val product = row.toStoredProduct()
        assertEquals(97.5, product.price, 0.0)
        assertEquals(row.product_prices.first(), product.price_info)
    }

    @Test
    fun `inventory payload maps config, freshness and balances`() {
        val snapshot = obj(
            """
            {"config":{"enforcement_enabled":true,"routing_status":"configured"},
             "fetched_at":"2026-10-27T08:00:00Z","is_stale":false,
             "stocks":{
               "SKU1":{"sku":"SKU1","status":"available","available":12,"recommended_source":"expected","expected_receipt_date":"2026-11-02"},
               "SKU2":{"status":"something_new","available":3}
             }}
            """.trimIndent()
        ).toInventorySnapshot()

        assertEquals(true, snapshot.config?.enforcementEnabled)
        assertEquals(PresellInventoryRoutingStatus.CONFIGURED, snapshot.config?.routingStatus)
        assertFalse(snapshot.isStale)
        assertEquals(ReservationSource.EXPECTED, snapshot.stocks.getValue("SKU1").recommendedSource)
        assertEquals(PresellStockStatus.NOT_FOUND, snapshot.stocks.getValue("SKU2").status)
        assertEquals("SKU2", snapshot.stocks.getValue("SKU2").sku)
    }

    @Test
    fun `an unknown routing status blocks like an unconfigured warehouse`() {
        val snapshot = obj("""{"config":{"enforcement_enabled":true,"routing_status":"new_state"},"stocks":{}}""").toInventorySnapshot()
        assertEquals(PresellInventoryRoutingStatus.WAREHOUSE_NOT_CONFIGURED, snapshot.config?.routingStatus)
        assertEquals(true, snapshot.isStale)
    }
}
