package com.illa.cashvan.feature.printer

import com.illa.cashvan.feature.orders.data.model.CreateOrderResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formatter class for generating invoice text content
 */
object InvoiceFormatter {

    private const val LINE_WIDTH = 32 // Characters per line for thermal printer
    private const val SEPARATOR = "--------------------------------"

    /**
     * Generate invoice text with static data for testing
     * @return Formatted invoice text
     */
    fun generateStaticInvoice(): String {
        return buildString {
            // Header
            appendCentered("INVOICE")
            appendCentered("Cash Van System")
            append(SEPARATOR)
            append("\n")

            // Company/Store Info (Static Data)
            append("Store: Main Branch\n")
            append("Address: 123 Commerce St\n")
            append("Phone: +20 123 456 7890\n")
            append(SEPARATOR)
            append("\n")

            // Invoice Details
            append("Invoice #: INV-2024-001\n")
            append("Date: ${getCurrentDateTime()}\n")
            append("Cashier: Ahmed Hassan\n")
            append(SEPARATOR)
            append("\n")

            // Customer/Merchant Info
            append("Customer: Al-Noor Store\n")
            append("Address: 45 Market Street\n")
            append("Phone: +20 111 222 3333\n")
            append(SEPARATOR)
            append("\n")

            // Items Header
            appendLine("Item             Qty    Price")
            append(SEPARATOR)
            append("\n")

            // Sample Items (Static Data)
            appendItem("Coca Cola 330ml", 10, 15.00)
            appendItem("Pepsi 330ml", 5, 14.50)
            appendItem("Water 500ml", 20, 5.00)
            appendItem("Chips Regular", 15, 8.00)
            append(SEPARATOR)
            append("\n")

            // Totals
            val subtotal = 387.50
            val tax = 38.75
            val total = 426.25

            appendRightAligned("Subtotal:", subtotal)
            appendRightAligned("Tax (10%):", tax)
            append(SEPARATOR)
            append("\n")
            appendRightAligned("TOTAL:", total, true)
            append(SEPARATOR)
            append("\n")

            // Payment Info
            append("Payment Method: Cash\n")
            append("Amount Paid: ${formatCurrency(500.00)}\n")
            append("Change: ${formatCurrency(73.75)}\n")
            append(SEPARATOR)
            append("\n")

            // Footer
            appendCentered("Thank you for your business!")
            appendCentered("Visit us again")
            append("\n")
            appendCentered(getCurrentDateTime())
            append("\n\n")
        }
    }

    /**
     * Generate invoice from order response
     * @param order CreateOrderResponse containing order details
     * @return Formatted invoice text
     */
    fun generateInvoiceFromOrder(order: CreateOrderResponse): String {
        return buildString {
            // Header
            appendCentered("INVOICE")
            appendCentered("Cash Van System")
            append(SEPARATOR)
            append("\n")

            // Company/Store Info
            append("Store: Cash Van\n")
            append("Address: Egypt\n")
            append(SEPARATOR)
            append("\n")

            // Invoice Details
            append("Invoice #: ${order.formatted_code}\n")
            append("Date: ${formatDate(order.created_at)}\n")
            append("Order ID: ${order.id}\n")
            append(SEPARATOR)
            append("\n")

            // Customer/Merchant Info
            append("Merchant ID: ${order.merchant_id}\n")
            append("Plan ID: ${order.plan_id}\n")
            append(SEPARATOR)
            append("\n")

            // Order Summary
            append("Total Quantity: ${order.total_sold_quantity}\n")
            append(SEPARATOR)
            append("\n")

            // Total
            appendRightAligned("TOTAL:", order.total_income.toDoubleOrNull() ?: 0.0, true)
            append(SEPARATOR)
            append("\n")

            // Footer
            appendCentered("Thank you for your business!")
            append("\n")
            appendCentered(getCurrentDateTime())
            append("\n\n")
        }
    }

    /**
     * Append centered text
     */
    private fun StringBuilder.appendCentered(text: String) {
        val padding = (LINE_WIDTH - text.length) / 2
        if (padding > 0) {
            append(" ".repeat(padding))
        }
        append(text)
        append("\n")
    }

    /**
     * Append item line
     */
    private fun StringBuilder.appendItem(name: String, quantity: Int, price: Double) {
        val itemName = if (name.length > 16) name.substring(0, 16) else name.padEnd(16)
        val qty = quantity.toString().padStart(3)
        val priceStr = formatCurrency(price * quantity).padStart(9)
        append("$itemName $qty $priceStr\n")
    }

    /**
     * Append right-aligned line with label and value
     */
    private fun StringBuilder.appendRightAligned(label: String, value: Double, bold: Boolean = false) {
        val valueStr = formatCurrency(value)
        val totalLength = label.length + valueStr.length + 1
        val padding = LINE_WIDTH - totalLength

        if (bold) append("**")
        append(label)
        if (padding > 0) {
            append(" ".repeat(padding))
        } else {
            append(" ")
        }
        append(valueStr)
        if (bold) append("**")
        append("\n")
    }

    /**
     * Format currency value
     */
    private fun formatCurrency(value: Double): String {
        return String.format(Locale.US, "%.2f EGP", value)
    }

    /**
     * Get current date and time
     */
    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    /**
     * Format date string
     */
    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }
}
