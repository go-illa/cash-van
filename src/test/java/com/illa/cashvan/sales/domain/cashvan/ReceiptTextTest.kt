package com.illa.cashvan.sales.domain.cashvan

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

// Kotlin-only (the PWA has no receiptText test). Expected receipts were
// produced by running the PWA's buildReceiptText in JavaScriptCore with
// TZ=Africa/Cairo.
class ReceiptTextTest {

    private val cairo = ZoneId.of("Africa/Cairo")

    private fun priceDetails(vat: Double, discount: Double, cashDiscount: Double) =
        RailsOrderPlanProductPriceDetails(RailsOrderPlanProductPriceTotals(discount, cashDiscount, vat))

    private fun order(
        formattedCode: String,
        createdAt: String,
        paymentType: RawPaymentType?,
        totalIncome: Double?,
        rebateValue: Double? = null,
        merchant: RailsMerchant? = null,
        lines: List<RailsOrderPlanProduct>? = null,
    ) = RailsOrder(
        id = "order-1",
        formattedCode = formattedCode,
        orderType = "cash_van",
        status = "fulfilled",
        paymentType = paymentType,
        totalIncome = totalIncome,
        createdAt = createdAt,
        rebateValue = rebateValue,
        merchant = merchant,
        orderPlanProducts = lines,
    )

    @Test
    fun `renders a full receipt with merchant, VAT, discounts and rebate`() {
        val receipt = buildReceiptText(
            order(
                formattedCode = "ORD-000123",
                createdAt = "2026-07-28T08:05:00Z",
                paymentType = RawPaymentType.Code(1),
                totalIncome = 1234.5,
                rebateValue = 12.5,
                merchant = RailsMerchant(id = "m1", name = "Merchant Name", signName = "سوبر ماركت النور", phoneNumber = "01012345678"),
                lines = listOf(
                    RailsOrderPlanProduct(
                        id = "1",
                        planProductId = "pp-1",
                        quantity = 2,
                        quantitySold = 2,
                        soldQuantity = 3,
                        totalIncome = 1000.0,
                        totalPriceDetails = priceDetails(vat = 140.0, discount = 10.0, cashDiscount = 5.0),
                        product = RailsProduct(id = "p1", name = "Pepsi Cola 330ml Can x24 Pack", sku = "SKU-1", imageUrl = null),
                    ),
                    RailsOrderPlanProduct(id = "2", planProductId = "pp-2", quantity = 12, quantitySold = 11, totalIncome = 234.5),
                ),
            ),
            ReceiptAgent(name = "Ahmed Ali", code = "AG-1"),
            cairo,
        )

        assertEquals(
            "================================\n   Front Door Logistic\n  فرونت دوور لوجستيك\n================================\nInvoice:              ORD-000123\nDate:          28/07/2026, 11:05\nAgent:                 Ahmed Ali\nPayment:            Credit / آجل\n--------------------------------\nClient: سوبر ماركت النور\nPhone: 01012345678\n================================\nItem                Qty   Total\n--------------------------------\nPepsi Cola 330ml C   3 1,000.00\n#pp-2               11   234.50\n--------------------------------\nSubtotal:           1,234.50 EGP\nVAT:                  140.00 EGP\nDiscount:             -15.00 EGP\nRebate:               -12.50 EGP\n================================\nTOTAL:              1,222.00 EGP\n================================\n\n",
            receipt,
        )
    }

    @Test
    fun `falls back to the agent code, the line subtotal and the Fawry key label`() {
        val receipt = buildReceiptText(
            order(
                formattedCode = "ORD-9",
                createdAt = "2026-01-05T23:30:00Z",
                paymentType = RawPaymentType.Key("fawry"),
                totalIncome = null,
                lines = listOf(RailsOrderPlanProduct(id = "1", planProductId = "abc", quantity = 1, quantitySold = 1, totalIncome = 50.0)),
            ),
            ReceiptAgent(name = "", code = "AG-7"),
            cairo,
        )

        assertEquals(
            "================================\n   Front Door Logistic\n  فرونت دوور لوجستيك\n================================\nInvoice:                   ORD-9\nDate:          06/01/2026, 01:30\nAgent:                      AG-7\nPayment:                   Fawry\n--------------------------------\nItem                Qty   Total\n--------------------------------\n#abc                 1    50.00\n--------------------------------\nSubtotal:              50.00 EGP\n================================\nTOTAL:                 50.00 EGP\n================================\n\n",
            receipt,
        )
    }

    @Test
    fun `handles invalid dates, overflowing labels, unknown codes and a rebate above the total`() {
        val receipt = buildReceiptText(
            order(
                formattedCode = "A-very-long-invoice-code-123456",
                createdAt = "garbage",
                // The PWA's label map has no numeric 2 key, so a Fawry code prints "Cash".
                paymentType = RawPaymentType.Code(2),
                totalIncome = 40.0,
                rebateValue = 100.0,
                merchant = RailsMerchant(id = "m1", name = "Shop", signName = "", phoneNumber = ""),
            ),
            ReceiptAgent(name = "X", code = "Y"),
            cairo,
        )

        assertEquals(
            "================================\n   Front Door Logistic\n  فرونت دوور لوجستيك\n================================\nInvoice: A-very-long-invoice-code-123456\nDate:               Invalid Date\nAgent:                         X\nPayment:                    Cash\n--------------------------------\nClient: Shop\n================================\nItem                Qty   Total\n--------------------------------\n--------------------------------\nSubtotal:               0.00 EGP\nRebate:              -100.00 EGP\n================================\nTOTAL:                  0.00 EGP\n================================\n\n",
            receipt,
        )
    }

    @Test
    fun `reads an offset-less timestamp as local time and keys string codes like a JS object`() {
        val receipt = buildReceiptText(
            order(
                formattedCode = "B",
                createdAt = "2026-07-28T08:05:00",
                paymentType = RawPaymentType.Key("0"),
                totalIncome = 1234567.891,
                rebateValue = 0.0,
            ),
            ReceiptAgent(name = "n", code = "c"),
            cairo,
        )

        assertEquals(
            "================================\n   Front Door Logistic\n  فرونت دوور لوجستيك\n================================\nInvoice:                       B\nDate:          28/07/2026, 08:05\nAgent:                         n\nPayment:             Cash / نقدي\n--------------------------------\nItem                Qty   Total\n--------------------------------\n--------------------------------\nSubtotal:               0.00 EGP\n================================\nTOTAL:          1,234,567.89 EGP\n================================\n\n",
            receipt,
        )
    }
}
