package com.illa.cashvan.feature.printer

import android.content.Context
import com.illa.cashvan.feature.orders.data.model.CreateOrderResponse
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ESC/POS command-based invoice formatter for thermal printers
 */
object EscPosInvoiceFormatter {

    private const val LINE_WIDTH = 32 // Characters per line

    /**
     * Load and format sample invoice from assets with proper Arabic encoding
     * Uses Windows-1256 which is standard for Arabic thermal printing
     */
    fun generateSampleInvoiceFromAssets(context: Context): ByteArray {
        val output = ByteArrayOutputStream()

        try {
            // Initialize printer
            output.write(EscPosCommands.INIT)

            // Load sample invoice text from assets
            val invoiceText = context.assets.open("SAMPLE_INVOICE.txt")
                .bufferedReader(Charset.forName("UTF-8"))
                .use { it.readText() }

            // Try multiple code pages for better compatibility
            // Code page 28 (0x1C) = Windows-1256 (Arabic)
            output.write(byteArrayOf(0x1B, 0x74, 0x1C.toByte()))

            // Also set the international character set
            output.write(byteArrayOf(0x1B, 0x52, 0x00.toByte()))

            // Convert text to Windows-1256 encoding
            val encodedText = invoiceText.toByteArray(Charset.forName("windows-1256"))
            output.write(encodedText)

            // Feed paper and cut
            output.write(EscPosCommands.LINE_FEED)
            output.write(EscPosCommands.LINE_FEED)
            output.write(EscPosCommands.LINE_FEED)

        } catch (e: Exception) {
            // Fallback to error message
            output.write(EscPosCommands.INIT)
            val errorMsg = "Error: ${e.message}\n"
            output.write(errorMsg.toByteArray(Charset.forName("UTF-8")))
        }

        return output.toByteArray()
    }

    /**
     * STEP 1: Simple test - Check if printer can print Arabic at all
     * Prints a simple Arabic test string
     */
    fun step1_testSimpleArabic(context: Context): ByteArray {
        val output = ByteArrayOutputStream()

        output.write(EscPosCommands.INIT)
        output.write("STEP 1: Simple Arabic Test\n".toByteArray())
        output.write("========================\n".toByteArray())

        // Simple Arabic text: "مرحبا" (Hello)
        val simpleArabic = "مرحبا"

        // Test with Windows-1256
        output.write("Win-1256: ".toByteArray())
        output.write(byteArrayOf(0x1B, 0x74, 0x1C.toByte()))
        output.write(simpleArabic.toByteArray(Charset.forName("windows-1256")))
        output.write("\n".toByteArray())

        // Test with UTF-8
        output.write("UTF-8: ".toByteArray())
        output.write(simpleArabic.toByteArray(Charset.forName("UTF-8")))
        output.write("\n".toByteArray())

        // Test with ISO-8859-6 (Arabic)
        output.write("ISO-8859-6: ".toByteArray())
        output.write(simpleArabic.toByteArray(Charset.forName("ISO-8859-6")))
        output.write("\n\n\n".toByteArray())

        return output.toByteArray()
    }

    /**
     * STEP 2: Test all available code pages (0-50)
     * This will help identify which code page your printer supports
     */
    fun step2_testAllCodePages(context: Context): ByteArray {
        val output = ByteArrayOutputStream()

        output.write(EscPosCommands.INIT)
        output.write("STEP 2: Code Page Tests\n".toByteArray())
        output.write("========================\n".toByteArray())

        val arabicText = "مرحبا العربية"

        // Test code pages 0-50
        val codePagesToTest = listOf(0, 6, 11, 15, 16, 17, 21, 22, 28, 36, 37, 38)

        for (cp in codePagesToTest) {
            output.write("CP$cp: ".toByteArray())
            output.write(byteArrayOf(0x1B, 0x74, cp.toByte()))
            output.write(arabicText.toByteArray(Charset.forName("windows-1256")))
            output.write("\n".toByteArray())
        }

        output.write("\n\n".toByteArray())
        return output.toByteArray()
    }

    /**
     * STEP 3: Test with Honeywell-specific commands
     * Uses specific commands for Honeywell printers
     */
    fun step3_testHoneywellSpecific(context: Context): ByteArray {
        val output = ByteArrayOutputStream()

        output.write(EscPosCommands.INIT)
        output.write("STEP 3: Honeywell Specific\n".toByteArray())
        output.write("===========================\n".toByteArray())

        // Load full invoice
        val invoiceText = try {
            context.assets.open("SAMPLE_INVOICE.txt")
                .bufferedReader(Charset.forName("UTF-8"))
                .use { it.readText() }
        } catch (e: Exception) {
            "Error loading file: ${e.message}"
        }

        // Honeywell might use different command set
        // Try ESC $ (alternative code page command)
        output.write(byteArrayOf(0x1B, 0x24, 0x11.toByte())) // Alternative command
        output.write(invoiceText.toByteArray(Charset.forName("windows-1256")))

        output.write("\n\n\n".toByteArray())
        return output.toByteArray()
    }

    /**
     * STEP 4: Print as raw bytes (no encoding conversion)
     * Tests if the file itself is the issue
     */
    fun step4_testRawBytes(context: Context): ByteArray {
        val output = ByteArrayOutputStream()

        output.write(EscPosCommands.INIT)
        output.write("STEP 4: Raw Bytes Test\n".toByteArray())
        output.write("=======================\n".toByteArray())

        // Set code page
        output.write(byteArrayOf(0x1B, 0x74, 0x1C.toByte()))

        // Read file as raw bytes
        try {
            val rawBytes = context.assets.open("SAMPLE_INVOICE.txt").readBytes()
            output.write(rawBytes)
        } catch (e: Exception) {
            output.write("Error: ${e.message}\n".toByteArray())
        }

        output.write("\n\n\n".toByteArray())
        return output.toByteArray()
    }

    /**
     * STEP 5: Print character by character with hex values
     * This will show exactly what bytes are being sent
     */
    fun step5_testCharacterByCharacter(context: Context): ByteArray {
        val output = ByteArrayOutputStream()

        output.write(EscPosCommands.INIT)
        output.write("STEP 5: Character Test\n".toByteArray())
        output.write("=======================\n".toByteArray())

        // Set Windows-1256 code page
        output.write(byteArrayOf(0x1B, 0x74, 0x1C.toByte()))

        val arabicText = "مرحبا"
        val bytes = arabicText.toByteArray(Charset.forName("windows-1256"))

        output.write("Text: ".toByteArray())
        output.write(bytes)
        output.write("\n".toByteArray())

        output.write("Hex: ".toByteArray())
        for (byte in bytes) {
            val hex = String.format("%02X ", byte)
            output.write(hex.toByteArray())
        }
        output.write("\n\n\n".toByteArray())

        return output.toByteArray()
    }

    /**
     * Generate ESC/POS invoice with static data for testing
     */
    fun generateStaticInvoice(): ByteArray {
        val output = ByteArrayOutputStream()

        // Initialize printer
        output.write(EscPosCommands.INIT)

        // Header - Bold and Centered
        output.write(EscPosCommands.ALIGN_CENTER)
        output.write(EscPosCommands.BOLD_ON)
        output.write(EscPosCommands.DOUBLE_HEIGHT_ON)
        output.write("INVOICE\n".toByteArray())
        output.write(EscPosCommands.NORMAL_TEXT)
        output.write(EscPosCommands.BOLD_OFF)

        output.write("Cash Van System\n".toByteArray())
        output.write(printLine())

        // Store info - Left aligned
        output.write(EscPosCommands.ALIGN_LEFT)
        output.write("Store: Main Branch\n".toByteArray())
        output.write("Address: 123 Commerce St\n".toByteArray())
        output.write("Phone: +20 123 456 7890\n".toByteArray())
        output.write(printLine())

        // Invoice details
        output.write("Invoice #: INV-2024-001\n".toByteArray())
        output.write("Date: ${getCurrentDateTime()}\n".toByteArray())
        output.write("Cashier: Ahmed Hassan\n".toByteArray())
        output.write(printLine())

        // Customer info
        output.write("Customer: Al-Noor Store\n".toByteArray())
        output.write("Address: 45 Market Street\n".toByteArray())
        output.write("Phone: +20 111 222 3333\n".toByteArray())
        output.write(printLine())

        // Items header
        output.write("Item             Qty    Price\n".toByteArray())
        output.write(printLine())

        // Items
        output.write(formatItem("Coca Cola 330ml", 10, 15.00))
        output.write(formatItem("Pepsi 330ml", 5, 14.50))
        output.write(formatItem("Water 500ml", 20, 5.00))
        output.write(formatItem("Chips Regular", 15, 8.00))
        output.write(printLine())

        // Totals
        val subtotal = 387.50
        val tax = 38.75
        val total = 426.25

        output.write(formatRightAlign("Subtotal:", subtotal))
        output.write(formatRightAlign("Tax (10%):", tax))
        output.write(printLine())

        // Total - Bold
        output.write(EscPosCommands.BOLD_ON)
        output.write(formatRightAlign("TOTAL:", total))
        output.write(EscPosCommands.BOLD_OFF)
        output.write(printLine())

        // Payment info
        output.write("Payment Method: Cash\n".toByteArray())
        output.write("Amount Paid: ${formatCurrency(500.00)}\n".toByteArray())
        output.write("Change: ${formatCurrency(73.75)}\n".toByteArray())
        output.write(printLine())

        // Footer - Centered
        output.write(EscPosCommands.ALIGN_CENTER)
        output.write("Thank you for your business!\n".toByteArray())
        output.write("Visit us again\n".toByteArray())
        output.write("\n${getCurrentDateTime()}\n".toByteArray())

        // Feed paper and cut
        output.write(EscPosCommands.LINE_FEED)
        output.write(EscPosCommands.LINE_FEED)
        output.write(EscPosCommands.LINE_FEED)

        return output.toByteArray()
    }

    /**
     * Generate ESC/POS invoice from order response
     */
    fun generateInvoiceFromOrder(order: CreateOrderResponse): ByteArray {
        val output = ByteArrayOutputStream()

        // Initialize printer
        output.write(EscPosCommands.INIT)

        // Header - Bold and Centered
        output.write(EscPosCommands.ALIGN_CENTER)
        output.write(EscPosCommands.BOLD_ON)
        output.write(EscPosCommands.DOUBLE_HEIGHT_ON)
        output.write("INVOICE\n".toByteArray())
        output.write(EscPosCommands.NORMAL_TEXT)
        output.write(EscPosCommands.BOLD_OFF)

        output.write("Cash Van System\n".toByteArray())
        output.write(printLine())

        // Store info - Left aligned
        output.write(EscPosCommands.ALIGN_LEFT)
        output.write("Store: Cash Van\n".toByteArray())
        output.write("Address: Egypt\n".toByteArray())
        output.write(printLine())

        // Invoice details
        output.write("Invoice #: ${order.formatted_code}\n".toByteArray())
        output.write("Date: ${formatDate(order.created_at)}\n".toByteArray())
        output.write("Order ID: ${order.id}\n".toByteArray())
        output.write(printLine())

        // Customer info
        output.write("Merchant ID: ${order.merchant_id}\n".toByteArray())
        output.write("Plan ID: ${order.plan_id}\n".toByteArray())
        output.write(printLine())

        // Order summary
        output.write("Total Quantity: ${order.total_sold_quantity}\n".toByteArray())
        output.write(printLine())

        // Total - Bold
        output.write(EscPosCommands.BOLD_ON)
        output.write(formatRightAlign("TOTAL:", order.total_income.toDoubleOrNull() ?: 0.0))
        output.write(EscPosCommands.BOLD_OFF)
        output.write(printLine())

        // Footer - Centered
        output.write(EscPosCommands.ALIGN_CENTER)
        output.write("Thank you for your business!\n".toByteArray())
        output.write("\n${getCurrentDateTime()}\n".toByteArray())

        // Feed paper and cut
        output.write(EscPosCommands.LINE_FEED)
        output.write(EscPosCommands.LINE_FEED)
        output.write(EscPosCommands.LINE_FEED)

        return output.toByteArray()
    }

    /**
     * Print separator line
     */
    private fun printLine(): ByteArray {
        return "--------------------------------\n".toByteArray()
    }

    /**
     * Format item line
     */
    private fun formatItem(name: String, quantity: Int, price: Double): ByteArray {
        val itemName = if (name.length > 16) name.substring(0, 16) else name.padEnd(16)
        val qty = quantity.toString().padStart(3)
        val priceStr = formatCurrency(price * quantity).padStart(9)
        return "$itemName $qty $priceStr\n".toByteArray()
    }

    /**
     * Format right-aligned line with label and value
     */
    private fun formatRightAlign(label: String, value: Double): ByteArray {
        val valueStr = formatCurrency(value)
        val totalLength = label.length + valueStr.length + 1
        val padding = LINE_WIDTH - totalLength

        val line = buildString {
            append(label)
            if (padding > 0) {
                append(" ".repeat(padding))
            } else {
                append(" ")
            }
            append(valueStr)
            append("\n")
        }

        return line.toByteArray()
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
