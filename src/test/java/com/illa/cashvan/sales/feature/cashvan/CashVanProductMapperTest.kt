package com.illa.cashvan.sales.feature.cashvan

import com.illa.cashvan.sales.feature.cashvan.data.mapper.resolveCashVanPrice
import com.illa.cashvan.sales.feature.cashvan.data.mapper.toCashVanProducts
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanPlanProductRow
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanPriceDetails
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanProductPriceRow
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanProductRef
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanUnitPrice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CashVanProductMapperTest {

    private val retailCairo = CashVanProductPriceRow(
        price_tier = "retail",
        governorate_id = "cairo",
        final_price = 114.0,
        base_price = 120.0,
        cash_discount = 4.0,
        vat_percentage = 14.0,
        vat_application = "exclusive",
        discounted = true
    )

    @Test
    fun `only an exact tier and governorate match prices the product`() {
        assertNull(resolveCashVanPrice(listOf(retailCairo), merchantGovernorateId = null, merchantPriceTier = "retail"))
        assertNull(resolveCashVanPrice(listOf(retailCairo), "giza", "retail"))
        assertNull(resolveCashVanPrice(listOf(retailCairo), "cairo", "wholesale"))
    }

    @Test
    fun `vat and discount are derived when price details are missing`() {
        val price = resolveCashVanPrice(listOf(retailCairo), "cairo", "retail")!!
        assertEquals(114.0 * 14 / 100, price.vatAmount, 1e-9)
        assertEquals(6.0, price.discountAmount, 1e-9)
        assertTrue(price.vatIsExclusive)
    }

    @Test
    fun `server price details win over derived values and base falls back to final`() {
        val row = retailCairo.copy(base_price = 0.0, price_details = CashVanPriceDetails(CashVanUnitPrice(discount_amount = 1.5, vat_amount = 10.0)))
        val price = resolveCashVanPrice(listOf(row), "cairo", "retail")!!
        assertEquals(114.0, price.basePrice, 0.0)
        assertEquals(10.0, price.vatAmount, 0.0)
        assertEquals(1.5, price.discountAmount, 0.0)
    }

    @Test
    fun `unpriced products and empty zero-priced ones are dropped`() {
        val rows = listOf(
            CashVanPlanProductRow(id = "pp1", product_id = "p1", cash_van_available_quantity = 3, product = CashVanProductRef(name = "Tea", sku = "T1"), product_prices = listOf(retailCairo)),
            CashVanPlanProductRow(id = "pp2", product_id = "p2", cash_van_available_quantity = 3, product_prices = emptyList()),
            CashVanPlanProductRow(id = "pp3", product_id = "p3", cash_van_available_quantity = -2, product_prices = listOf(retailCairo.copy(final_price = 0.0, cash_discount = 0.0)))
        )
        val products = rows.toCashVanProducts("cairo", "retail")
        assertEquals(listOf("pp1"), products.map { it.planProductId })
        assertEquals(110.0, products.single().price, 0.0)
        assertFalse(products.any { it.cashVanAvailable < 0 })
    }
}
