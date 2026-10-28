package com.illa.cashvan.sales.domain.reconciliation

import com.illa.cashvan.sales.domain.cashvan.CashvanPlanType
import com.illa.cashvan.sales.domain.cashvan.RailsOrder
import com.illa.cashvan.sales.domain.cashvan.RailsPlanProduct
import com.illa.cashvan.sales.domain.cashvan.RailsPreSellSalesOrder
import com.illa.cashvan.sales.domain.cashvan.RailsProduct
import com.illa.cashvan.sales.domain.cashvan.RawPaymentType
import com.illa.cashvan.sales.domain.checkout.PaymentType
import com.illa.cashvan.sales.domain.reconciliation.DailyReconciliationPdfExportError.Cancelled
import com.illa.cashvan.sales.domain.reconciliation.DailyReconciliationPdfExportError.Failed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CancellationException

// Ported from the PWA's src/lib/dailyReconciliation.test.ts.
class DailyReconciliationTest {

    private fun key(value: String) = RawPaymentType.Key(value)

    private fun code(value: Int) = RawPaymentType.Code(value)

    private fun makeOrder(
        id: String = "order-1",
        orderType: String? = "cash_van",
        status: String = "fulfilled",
        paymentType: RawPaymentType? = code(0),
        totalIncome: Double? = 0.0,
        salesOrder: RailsPreSellSalesOrder? = null,
    ) = RailsOrder(
        id = id,
        formattedCode = "ORD-1",
        orderType = orderType,
        status = status,
        paymentType = paymentType,
        totalIncome = totalIncome,
        createdAt = "2026-07-28T08:00:00Z",
        salesOrder = salesOrder,
    )

    private fun makeSalesOrder(paymentType: String = "cash") =
        RailsPreSellSalesOrder(id = "sales-order-1", status = "fulfilled", paymentType = paymentType)

    private val defaultProduct = RailsProduct(id = "product-1", name = "Product", sku = "SKU-1", imageUrl = null)

    private fun makePlanProduct(
        id: String = "pp-1",
        assigned: Double? = 0.0,
        sold: Double? = 0.0,
        totalIncome: Double? = 0.0,
        totalAvailable: Double? = 0.0,
        preSellAvailable: Double? = 0.0,
        cashVanAvailable: Double? = 0.0,
        preSellSold: Double? = 0.0,
        cashVanSold: Double? = 0.0,
        product: RailsProduct? = defaultProduct,
    ) = RailsPlanProduct(
        id = id,
        productId = "product-1",
        assignedQuantity = assigned,
        soldQuantity = sold,
        totalIncome = totalIncome,
        totalAvailableQuantity = totalAvailable,
        preSellAvailableQuantity = preSellAvailable,
        cashVanAvailableQuantity = cashVanAvailable,
        preSellSoldQuantity = preSellSold,
        cashVanSoldQuantity = cashVanSold,
        product = product,
    )

    private fun totals(cash: Double, credit: Double, fawry: Double) = DailyReconciliationTotals(cash, credit, fawry)

    // classifyDailyReconciliationPdfExportError

    @Test
    fun `classifies cancellations as cancelled whatever the concrete exception type`() {
        class ShareSheetDismissed : CancellationException("dismissed")

        assertEquals(Cancelled, classifyDailyReconciliationPdfExportError(CancellationException("aborted")))
        assertEquals(Cancelled, classifyDailyReconciliationPdfExportError(ShareSheetDismissed()))
    }

    @Test
    fun `preserves the platform error type for failed-export analytics`() {
        assertEquals(
            Failed("SecurityException"),
            classifyDailyReconciliationPdfExportError(SecurityException("blocked")),
        )
    }

    @Test
    fun `uses stable fallbacks for nameless errors and non-error throws`() {
        val unnamedError = object : RuntimeException("missing name") {}

        assertEquals(Failed("IllegalArgumentException"), classifyDailyReconciliationPdfExportError(IllegalArgumentException("bad value")))
        assertEquals(Failed("Error"), classifyDailyReconciliationPdfExportError(unnamedError))
        assertEquals(Failed("string"), classifyDailyReconciliationPdfExportError("failed"))
        assertEquals(Failed("object"), classifyDailyReconciliationPdfExportError(null))
    }

    // normalizePaymentType / normalizeOrderType

    @Test
    fun `reads the real API string shapes`() {
        assertEquals(PaymentType.CASH, normalizeReconciliationPaymentType(key("cash")))
        assertEquals(PaymentType.CREDIT, normalizeReconciliationPaymentType(key("credit")))
        assertEquals(PaymentType.FAWRY, normalizeReconciliationPaymentType(key("fawry")))
        assertEquals(ReconciliationOrderType.CASH_VAN, normalizeOrderType("cash_van"))
        assertEquals(ReconciliationOrderType.PRE_SELL, normalizeOrderType("pre_sell"))
    }

    @Test
    fun `defaults defensively for unrecognized values`() {
        assertEquals(PaymentType.CASH, normalizeReconciliationPaymentType(null))
        assertEquals(ReconciliationOrderType.CASH_VAN, normalizeOrderType(null))
    }

    // Kotlin-only: numeric codes, case-insensitivity and the warning hook.
    @Test
    fun `reads numeric codes and mixed-case keys, and reports unknown values`() {
        val warnings = mutableListOf<String>()

        assertEquals(PaymentType.CREDIT, normalizeReconciliationPaymentType(code(1)))
        assertEquals(PaymentType.FAWRY, normalizeReconciliationPaymentType(code(2)))
        assertEquals(PaymentType.CREDIT, normalizeReconciliationPaymentType(key("CREDIT")))
        assertEquals(PaymentType.CASH, normalizeReconciliationPaymentType(key("1"), warnings::add))
        assertEquals(PaymentType.CASH, normalizeReconciliationPaymentType(code(7), warnings::add))
        assertEquals(ReconciliationOrderType.CASH_VAN, normalizeOrderType("retail", warnings::add))

        assertEquals(
            listOf(
                "[dailyReconciliation] Unrecognized payment_type \"1\" — treating as cash",
                "[dailyReconciliation] Unrecognized payment_type 7 — treating as cash",
                "[dailyReconciliation] Unrecognized order_type \"retail\" — treating as cash_van",
            ),
            warnings,
        )
    }

    // resolveOrderPaymentType

    @Test
    fun `uses payment_type directly for cash_van orders`() {
        val order = makeOrder(orderType = "cash_van", paymentType = key("fawry"))
        assertEquals(PaymentType.FAWRY, resolveOrderPaymentType(order))
    }

    @Test
    fun `uses the originating sales_order's payment_type for pre_sell orders`() {
        // The fulfillment Order row's own payment_type defaults to cash and is never set during fulfillment.
        val order = makeOrder(
            orderType = "pre_sell",
            status = "fulfilled",
            paymentType = code(0),
            salesOrder = makeSalesOrder(paymentType = "credit"),
        )

        assertEquals(PaymentType.CREDIT, resolveOrderPaymentType(order))
    }

    @Test
    fun `falls back to the order's own payment_type if a pre_sell order has no sales_order attached`() {
        val order = makeOrder(orderType = "pre_sell", status = "fulfilled", paymentType = key("fawry"), salesOrder = null)
        assertEquals(PaymentType.FAWRY, resolveOrderPaymentType(order))
    }

    // filterReconciledOrders

    @Test
    fun `drops canceled cash_van orders and keeps fulfilled ones (DIS-407)`() {
        val orders = listOf(
            makeOrder(id = "1", orderType = "cash_van", status = "fulfilled"),
            makeOrder(id = "2", orderType = "cash_van", status = "canceled"),
        )
        assertEquals(listOf("1"), filterReconciledOrders(orders).map { it.id })
    }

    @Test
    fun `drops canceled, ongoing and voided pre_sell orders and keeps fulfilled and partially_fulfilled ones (DIS-407)`() {
        val orders = listOf(
            makeOrder(id = "1", orderType = "pre_sell", status = "fulfilled"),
            makeOrder(id = "2", orderType = "pre_sell", status = "partially_fulfilled"),
            makeOrder(id = "3", orderType = "pre_sell", status = "canceled"),
            makeOrder(id = "4", orderType = "pre_sell", status = "ongoing"),
            makeOrder(id = "5", orderType = "pre_sell", status = "voided"),
        )
        assertEquals(listOf("1", "2"), filterReconciledOrders(orders).map { it.id })
    }

    // computeMoneyBreakdown

    @Test
    fun `reads pre_sell payment_type from the sales_order, not the fulfillment order's unreliable default`() {
        // Regression test for the cash-only bug.
        val orders = listOf(
            makeOrder(
                id = "1",
                orderType = "pre_sell",
                status = "fulfilled",
                paymentType = code(0),
                totalIncome = 300.0,
                salesOrder = makeSalesOrder(paymentType = "credit"),
            ),
            makeOrder(
                id = "2",
                orderType = "pre_sell",
                status = "fulfilled",
                paymentType = code(0),
                totalIncome = 80.0,
                salesOrder = makeSalesOrder(paymentType = "fawry"),
            ),
        )

        assertEquals(totals(cash = 0.0, credit = 300.0, fawry = 80.0), computeMoneyBreakdown(orders))
    }

    @Test
    fun `merges cash_van cash and fawry sales into the flat totals`() {
        val orders = listOf(
            makeOrder(id = "1", orderType = "cash_van", paymentType = key("cash"), totalIncome = 100.0),
            makeOrder(id = "2", orderType = "cash_van", paymentType = key("fawry"), totalIncome = 30.0),
        )

        assertEquals(totals(cash = 100.0, credit = 0.0, fawry = 30.0), computeMoneyBreakdown(orders))
    }

    @Test
    fun `merges fulfilled and partially_fulfilled pre_sell orders into the same flat totals`() {
        val orders = listOf(
            makeOrder(id = "1", orderType = "cash_van", paymentType = key("cash"), totalIncome = 100.0),
            makeOrder(id = "2", orderType = "pre_sell", status = "fulfilled", paymentType = key("credit"), totalIncome = 200.0),
            makeOrder(id = "3", orderType = "pre_sell", status = "partially_fulfilled", paymentType = key("cash"), totalIncome = 50.0),
            makeOrder(id = "4", orderType = "pre_sell", status = "fulfilled", paymentType = key("fawry"), totalIncome = 20.0),
        )

        // cash: 100 (cash_van) + 50 (pre_sell) = 150; credit: 200 (pre_sell only); fawry: 20
        assertEquals(totals(cash = 150.0, credit = 200.0, fawry = 20.0), computeMoneyBreakdown(orders))
    }

    @Test
    fun `excludes pre_sell orders that have not been delivered yet`() {
        val orders = listOf(
            makeOrder(id = "1", orderType = "pre_sell", status = "ongoing", paymentType = key("credit"), totalIncome = 999.0),
            makeOrder(id = "2", orderType = "pre_sell", status = "canceled", paymentType = key("credit"), totalIncome = 999.0),
            makeOrder(id = "3", orderType = "pre_sell", status = "voided", paymentType = key("credit"), totalIncome = 999.0),
        )

        assertEquals(totals(cash = 0.0, credit = 0.0, fawry = 0.0), computeMoneyBreakdown(orders))
    }

    @Test
    fun `excludes a cash_van order that is not fulfilled`() {
        val orders = listOf(
            makeOrder(id = "1", orderType = "cash_van", status = "unfulfilled", paymentType = key("cash"), totalIncome = 999.0),
        )

        assertEquals(totals(cash = 0.0, credit = 0.0, fawry = 0.0), computeMoneyBreakdown(orders))
    }

    @Test
    fun `counts a cash_van order with payment_type credit under credit`() {
        // Diverges from the TS test "treats a cash_van order with payment_type=credit defensively as
        // cash", which is stale: PWA commit be64d04 (CashVan Credit payment type) removed that
        // fold-into-cash rule from computeMoneyBreakdown but left the test expecting { cash: 75 }.
        val orders = listOf(makeOrder(id = "1", orderType = "cash_van", paymentType = key("credit"), totalIncome = 75.0))

        assertEquals(totals(cash = 0.0, credit = 75.0, fawry = 0.0), computeMoneyBreakdown(orders))
    }

    @Test
    fun `returns zeros for an empty order list (zero-sales scenario)`() {
        assertEquals(totals(cash = 0.0, credit = 0.0, fawry = 0.0), computeMoneyBreakdown(emptyList()))
    }

    // computePlanProductBreakdown

    @Test
    fun `matches the hybrid-plan mock's SKU split (assigned 4, pre_sell sold 3, cash_van available 1)`() {
        val planProducts = listOf(
            makePlanProduct(
                id = "9fe6e0ba",
                assigned = 4.0,
                sold = 3.0,
                totalAvailable = 1.0,
                totalIncome = 588.0,
                preSellSold = 3.0,
                cashVanSold = 0.0,
                preSellAvailable = 0.0,
                cashVanAvailable = 1.0,
                product = RailsProduct(id = "p1", name = "ويليز بريميوم كولا", sku = "EG-562062645-13", imageUrl = null),
            ),
        )

        val row = computePlanProductBreakdown(planProducts, CashvanPlanType.HYBRID).single()
        assertEquals(4.0, row.assignedQuantity, 0.0)
        assertEquals(3.0, row.soldQuantity, 0.0)
        assertEquals(1.0, row.remainingQuantity, 0.0)
        assertEquals(588.0, row.totalValue, 0.0)
        assertTrue(row.hasChannelBreakdown)
        assertEquals(3.0, row.soldPreSell, 0.0)
        assertEquals(0.0, row.soldCashVan, 0.0)
        assertEquals(0.0, row.availablePreSell, 0.0)
        assertEquals(1.0, row.availableCashVan, 0.0)
        // Per-channel "loaded" (sold + still available) sums back to the shared assignedQuantity: (3+0) + (0+1) = 4.
        assertEquals(3.0, row.loadedPreSell, 0.0)
        assertEquals(1.0, row.loadedCashVan, 0.0)
        assertEquals(row.assignedQuantity, row.loadedPreSell + row.loadedCashVan, 0.0)
    }

    @Test
    fun `equals the full initial load when nothing has been sold (zero-sales scenario)`() {
        val planProducts = listOf(makePlanProduct(assigned = 40.0, sold = 0.0, totalAvailable = 40.0, cashVanAvailable = 40.0))

        val row = computePlanProductBreakdown(planProducts, CashvanPlanType.CASH_VAN).single()
        assertEquals(40.0, row.availableCashVan, 0.0)
        assertEquals(40.0, row.loadedCashVan, 0.0)
    }

    // Production cash_van plans missing the per-channel sold fields (DIS-409): the *_sold_quantity
    // split fields are absent entirely; only the plan-wide aggregates are present.
    private fun makeProductionCashVanProduct(
        id: String = "pp-1",
        assigned: Double,
        sold: Double,
        totalAvailable: Double,
        cashVanAvailable: Double,
    ) = makePlanProduct(
        id = id,
        assigned = assigned,
        sold = sold,
        totalAvailable = totalAvailable,
        cashVanAvailable = cashVanAvailable,
        preSellSold = null,
        cashVanSold = null,
        preSellAvailable = 0.0,
    )

    @Test
    fun `DIS-409 falls back to the aggregate fields instead of NaN (3-0-3 example)`() {
        val planProducts = listOf(makeProductionCashVanProduct(assigned = 3.0, sold = 0.0, totalAvailable = 3.0, cashVanAvailable = 3.0))

        val row = computePlanProductBreakdown(planProducts, CashvanPlanType.CASH_VAN).single()
        assertEquals(0.0, row.soldCashVan, 0.0)
        assertEquals(3.0, row.availableCashVan, 0.0)
        assertEquals(3.0, row.loadedCashVan, 0.0)
        assertEquals(0.0, row.soldPreSell, 0.0)
        assertEquals(0.0, row.availablePreSell, 0.0)
        assertEquals(0.0, row.loadedPreSell, 0.0)
        assertTrue(row.hasChannelBreakdown)
        assertFalse(row.loadedCashVan.isNaN())
    }

    @Test
    fun `DIS-409 falls back correctly for the 5-1-4 example`() {
        val planProducts = listOf(makeProductionCashVanProduct(assigned = 5.0, sold = 1.0, totalAvailable = 4.0, cashVanAvailable = 4.0))

        val row = computePlanProductBreakdown(planProducts, CashvanPlanType.CASH_VAN).single()
        assertEquals(1.0, row.soldCashVan, 0.0)
        assertEquals(4.0, row.availableCashVan, 0.0)
        assertEquals(5.0, row.loadedCashVan, 0.0)
    }

    @Test
    fun `DIS-409 falls back correctly when fully sold (5-5-0)`() {
        val planProducts = listOf(makeProductionCashVanProduct(assigned = 5.0, sold = 5.0, totalAvailable = 0.0, cashVanAvailable = 0.0))

        val row = computePlanProductBreakdown(planProducts, CashvanPlanType.CASH_VAN).single()
        assertEquals(5.0, row.soldCashVan, 0.0)
        assertEquals(0.0, row.availableCashVan, 0.0)
        assertEquals(5.0, row.loadedCashVan, 0.0)
    }

    @Test
    fun `DIS-409 reconciles the reported production totals across a full plan (240-126-114)`() {
        val planProducts = listOf(
            makeProductionCashVanProduct(id = "pp-a", assigned = 100.0, sold = 60.0, totalAvailable = 40.0, cashVanAvailable = 40.0),
            makeProductionCashVanProduct(id = "pp-b", assigned = 140.0, sold = 66.0, totalAvailable = 74.0, cashVanAvailable = 74.0),
        )

        val rows = computePlanProductBreakdown(planProducts, CashvanPlanType.CASH_VAN)
        val totalLoaded = rows.sumOf { it.loadedCashVan }
        val totalSold = rows.sumOf { it.soldCashVan }
        val totalAvailable = rows.sumOf { it.availableCashVan }

        assertEquals(240.0, totalLoaded, 0.0)
        assertEquals(126.0, totalSold, 0.0)
        assertEquals(114.0, totalAvailable, 0.0)
        assertEquals(totalAvailable, totalLoaded - totalSold, 0.0)
    }

    @Test
    fun `DIS-409 keeps aggregate fields authoritative even if redundant cash_van split fields disagree`() {
        val planProducts = listOf(
            makePlanProduct(assigned = 10.0, sold = 7.0, totalAvailable = 3.0, cashVanSold = 999.0, cashVanAvailable = 999.0),
        )

        val row = computePlanProductBreakdown(planProducts, CashvanPlanType.CASH_VAN).single()
        assertEquals(7.0, row.soldCashVan, 0.0)
        assertEquals(3.0, row.availableCashVan, 0.0)
    }

    // Production hybrid plans missing the per-channel sold fields.
    private fun makeProductionHybridProduct(
        id: String = "pp-1",
        assigned: Double,
        sold: Double,
        totalAvailable: Double,
        preSellAvailable: Double = 0.0,
        cashVanAvailable: Double = 0.0,
        preSellSold: Double? = null,
    ) = makePlanProduct(
        id = id,
        assigned = assigned,
        sold = sold,
        totalAvailable = totalAvailable,
        preSellAvailable = preSellAvailable,
        cashVanAvailable = cashVanAvailable,
        preSellSold = preSellSold,
        cashVanSold = null,
    )

    @Test
    fun `hybrid keeps authoritative aggregate quantities for fully sold and unsold SKUs`() {
        val rows = computePlanProductBreakdown(
            listOf(
                makeProductionHybridProduct(id = "maxi-250-2", assigned = 4.0, sold = 4.0, totalAvailable = 0.0),
                makeProductionHybridProduct(id = "dinamo-250", assigned = 2.0, sold = 0.0, totalAvailable = 2.0, cashVanAvailable = 2.0),
            ),
            CashvanPlanType.HYBRID,
        )

        assertEquals(
            listOf(Triple(4.0, 4.0, 0.0), Triple(2.0, 0.0, 2.0)),
            rows.map { Triple(it.assignedQuantity, it.soldQuantity, it.remainingQuantity) },
        )
        assertTrue(rows.all { !it.hasChannelBreakdown })
        assertTrue(rows.all { it.loadedCashVan == 0.0 && it.loadedPreSell == 0.0 })
    }

    @Test
    fun `hybrid does not treat a partial channel split as complete`() {
        val row = computePlanProductBreakdown(
            listOf(makeProductionHybridProduct(assigned = 5.0, sold = 3.0, totalAvailable = 2.0, preSellSold = 1.0)),
            CashvanPlanType.HYBRID,
        ).single()

        assertFalse(row.hasChannelBreakdown)
        assertEquals(5.0, row.assignedQuantity, 0.0)
        assertEquals(3.0, row.soldQuantity, 0.0)
        assertEquals(2.0, row.remainingQuantity, 0.0)
    }

    @Test
    fun `uses aggregate quantities for a pre_sell-only plan when split sold fields are absent`() {
        val row = computePlanProductBreakdown(
            listOf(makePlanProduct(assigned = 7.0, sold = 5.0, totalAvailable = 2.0, preSellSold = null, cashVanSold = null)),
            CashvanPlanType.PRE_SELL,
        ).single()

        assertTrue(row.hasChannelBreakdown)
        assertEquals(7.0, row.loadedPreSell, 0.0)
        assertEquals(5.0, row.soldPreSell, 0.0)
        assertEquals(2.0, row.availablePreSell, 0.0)
        assertEquals(0.0, row.loadedCashVan, 0.0)
    }

    // Kotlin-only: an unknown plan type takes the hybrid path, like an unexpected TS string.
    @Test
    fun `treats an unknown plan type like a hybrid plan`() {
        val row = computePlanProductBreakdown(
            listOf(makePlanProduct(assigned = 4.0, sold = 3.0, totalAvailable = 1.0, preSellSold = 3.0, cashVanSold = 0.0, cashVanAvailable = 1.0)),
            CashvanPlanType.fromWire("something_new"),
        ).single()

        assertTrue(row.hasChannelBreakdown)
        assertEquals(3.0, row.loadedPreSell, 0.0)
        assertEquals(1.0, row.loadedCashVan, 0.0)
    }

    // groupRowsByPrincipal

    private fun rowFor(id: String, name: String, sku: String) = computePlanProductBreakdown(
        listOf(makePlanProduct(id = id, product = RailsProduct(id = "p-$id", name = name, sku = sku, imageUrl = null))),
        CashvanPlanType.HYBRID,
    ).single()

    @Test
    fun `groups rows under their matched principal name, sorted alphabetically`() {
        val rowA = rowFor("a", "A", "SKU-A")
        val rowB = rowFor("b", "B", "SKU-B")
        val rowC = rowFor("c", "C", "SKU-C")

        val groups = groupRowsByPrincipal(listOf(rowA, rowB, rowC), mapOf("SKU-A" to "Zeta Co", "SKU-B" to "Alpha Co"))

        assertEquals(listOf("Alpha Co", "Zeta Co", "منتجات أخرى"), groups.map { it.principalName })
        assertEquals(listOf(rowB), groups[0].rows)
        assertEquals(listOf(rowA), groups[1].rows)
        assertEquals(listOf(rowC), groups[2].rows)
    }

    @Test
    fun `buckets every row under the other-products label when no principal map is available`() {
        val row = computePlanProductBreakdown(listOf(makePlanProduct()), CashvanPlanType.HYBRID).single()
        assertEquals(listOf(PrincipalGroup("منتجات أخرى", listOf(row))), groupRowsByPrincipal(listOf(row), null))
    }

    // Kotlin-only; expected order taken from JavaScriptCore's localeCompare(b, 'ar').
    @Test
    fun `sorts Arabic principal names with the Arabic collation and keeps the other bucket last`() {
        val names = listOf("بيبسي", "ألفا", "زيت", "إيديتا", "آبا")
        val rows = names.mapIndexed { index, _ -> rowFor("r$index", "P$index", "SKU-$index") }
        val principalBySku = names.mapIndexed { index, name -> "SKU-$index" to name }.toMap()
        val unmatched = rowFor("x", "X", "")

        val groups = groupRowsByPrincipal(listOf(unmatched) + rows, principalBySku)

        assertEquals(
            listOf("آبا", "ألفا", "إيديتا", "بيبسي", "زيت", OTHER_PRINCIPAL_LABEL),
            groups.map { it.principalName },
        )
    }
}
