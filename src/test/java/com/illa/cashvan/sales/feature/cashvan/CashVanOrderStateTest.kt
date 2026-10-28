package com.illa.cashvan.sales.feature.cashvan

import com.illa.cashvan.sales.domain.cashvan.CashvanCartItem
import com.illa.cashvan.sales.domain.cashvan.CashvanPriceDetails
import com.illa.cashvan.sales.domain.cashvan.RawPaymentType
import com.illa.cashvan.sales.feature.cashvan.data.mapper.toRailsOrder
import com.illa.cashvan.sales.feature.cashvan.data.model.PlanDetailOrderRow
import com.illa.cashvan.sales.feature.cashvan.data.model.VisitRecommendationsRow
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.CashVanOrderUiState
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CashVanOrderStateTest {

    private fun item(id: String, final: Double, cash: Double, vat: Double, exclusive: Boolean, quantity: Int) = CashvanCartItem(
        planProductId = id,
        productId = "p-$id",
        productName = "Product $id",
        sku = "SKU-$id",
        imageUrl = null,
        quantity = quantity,
        maxQuantity = 10,
        unitPrice = final - cash,
        priceDetails = CashvanPriceDetails(final, final, 14.0, vat, exclusive, 0.0, cash)
    )

    private val cart = listOf(
        item("1", final = 100.0, cash = 5.0, vat = 14.0, exclusive = true, quantity = 2),
        item("2", final = 50.0, cash = 0.0, vat = 7.0, exclusive = false, quantity = 1)
    )

    @Test
    fun `totals add exclusive VAT to unit prices and subtract the rebate`() {
        val state = CashVanOrderUiState(cart = cart, rebateInput = "10")
        assertEquals(240.0, state.cartValue, 1e-9)
        assertEquals(250.0, state.subtotalWithVat, 1e-9)
        assertEquals(35.0, state.vatTotal, 1e-9)
        assertEquals(10.0, state.cashDiscountTotal, 1e-9)
        assertEquals(268.0, state.totalBeforeRebate, 1e-9)
        assertEquals(258.0, state.total, 1e-9)
    }

    @Test
    fun `a rebate above the total is rejected and not subtracted`() {
        val state = CashVanOrderUiState(cart = cart, rebateInput = "1000")
        assertNotNull(state.rebate.errorMessage)
        assertEquals(268.0, state.total, 1e-9)
    }

    @Test
    fun `spend gauge is capped at 100 and needs a prediction`() {
        assertEquals(48, CashVanOrderUiState(cart = cart, recommendations = VisitRecommendationsRow(predicted_spending = 500.0)).spendPercent)
        assertEquals(100, CashVanOrderUiState(cart = cart, recommendations = VisitRecommendationsRow(predicted_spending = 100.0)).spendPercent)
        assertNull(CashVanOrderUiState(cart = cart, recommendations = VisitRecommendationsRow(predicted_spending = 0.0)).spendPercent)
    }

    @Test
    fun `payment type numbers and strings stay distinct`() {
        val byCode = PlanDetailOrderRow(id = "o1", payment_type = JsonPrimitive(1)).toRailsOrder()
        val byKey = PlanDetailOrderRow(id = "o2", payment_type = JsonPrimitive("1")).toRailsOrder()
        assertEquals(RawPaymentType.Code(1), byCode.paymentType)
        assertEquals(RawPaymentType.Key("1"), byKey.paymentType)
        assertNull(PlanDetailOrderRow(id = "o3").toRailsOrder().paymentType)
    }
}
