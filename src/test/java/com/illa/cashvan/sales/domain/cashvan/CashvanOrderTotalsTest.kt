package com.illa.cashvan.sales.domain.cashvan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported from the PWA's src/lib/cashvanOrderTotals.test.ts. Serialized (string)
// totals go through finiteNumberOrNull(String), the TS `finiteNumber`.
class CashvanOrderTotalsTest {

    @Test
    fun `normalizes the serialized order total`() {
        assertEquals(125.5, cashvanOrderTotal(125.5, null), 0.0)
        assertEquals(125.5, cashvanOrderTotal(finiteNumberOrNull("125.50"), null), 0.0)
        assertEquals(0.0, cashvanOrderTotal(0.0, null), 0.0)
        assertEquals(0.0, cashvanOrderTotal(finiteNumberOrNull("0"), null), 0.0)
    }

    @Test
    fun `falls back to the sum of numeric line-item totals`() {
        val total = cashvanOrderTotal(
            totalIncome = null,
            lineItemTotals = listOf(finiteNumberOrNull("100.25"), 49.75, finiteNumberOrNull("invalid")),
        )
        assertEquals(150.0, total, 0.0)
    }

    @Test
    fun `does not concatenate string totals`() {
        val total = listOf(
            cashvanOrderTotal(finiteNumberOrNull("100"), null),
            cashvanOrderTotal(finiteNumberOrNull("25"), null),
        ).sum()

        assertEquals(125.0, total, 0.0)
    }

    @Test
    fun `uses zero for empty and non-finite values`() {
        assertEquals(0.0, finiteNumberOrZero(""), 0.0)
        assertEquals(0.0, finiteNumberOrZero("not-a-number"), 0.0)
        assertEquals(0.0, finiteNumberOrZero(Double.POSITIVE_INFINITY), 0.0)
    }

    // Kotlin-only: the RailsOrder overload and JS Number() string semantics.

    @Test
    fun `totals a RailsOrder from its line items when the serialized total is missing or non-finite`() {
        val lines = listOf(
            RailsOrderPlanProduct(id = "1", planProductId = "pp-1", totalIncome = 10.5),
            RailsOrderPlanProduct(id = "2", planProductId = "pp-2", totalIncome = Double.NaN),
            RailsOrderPlanProduct(id = "3", planProductId = "pp-3", totalIncome = null),
        )
        val order = RailsOrder(
            id = "o",
            formattedCode = "ORD",
            orderType = "cash_van",
            status = "fulfilled",
            paymentType = null,
            totalIncome = Double.NaN,
            createdAt = "2026-07-28T08:00:00Z",
            orderPlanProducts = lines,
        )

        assertEquals(10.5, cashvanOrderTotal(order), 0.0)
        assertEquals(99.0, cashvanOrderTotal(order.copy(totalIncome = 99.0)), 0.0)
        assertEquals(0.0, cashvanOrderTotal(order.copy(orderPlanProducts = null)), 0.0)
    }

    @Test
    fun `parses strings like JS Number`() {
        // Expected values taken from JavaScriptCore's Number(string).
        mapOf(
            " 12 " to 12.0,
            "0." to 0.0,
            ".5" to 0.5,
            "5." to 5.0,
            "1e2" to 100.0,
            "+5" to 5.0,
            "0x10" to 16.0,
            "0b101" to 5.0,
            "0o17" to 15.0,
            " 12 " to 12.0,
            "" to 0.0,
            " " to 0.0,
        ).forEach { (input, expected) -> assertEquals("Number(\"$input\")", expected, jsStringToNumber(input), 0.0) }

        assertEquals(Double.POSITIVE_INFINITY, jsStringToNumber("Infinity"), 0.0)
        assertEquals(Double.NEGATIVE_INFINITY, jsStringToNumber("-Infinity"), 0.0)
        listOf("-0x10", "1_000", "infinity", "1e", "1d", "NaN", "12abc", "0x1g", "0b102").forEach { input ->
            assertTrue("Number(\"$input\") is NaN", jsStringToNumber(input).isNaN())
        }
    }

    @Test
    fun `treats blank serialized numbers as missing rather than zero`() {
        assertNull(finiteNumberOrNull("   "))
        assertNull(finiteNumberOrNull(null as String?))
        assertNull(finiteNumberOrNull("Infinity"))
        assertEquals(12.0, finiteNumberOrNull(" 12 ") ?: Double.NaN, 0.0)
    }
}
