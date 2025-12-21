package com.illa.cashvan.feature.printer

import android.content.Context
import com.illa.cashvan.feature.orders.data.model.CreateOrderResponse
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * CPCL (Common Printing Command Language) invoice formatter for Honeywell printers
 * CPCL is Honeywell's preferred printing language
 */
object CpclInvoiceFormatter {

    /**
     * Load and format sample invoice from assets using CPCL commands
     */
    fun generateSampleInvoiceFromAssets(context: Context): ByteArray {
        val output = ByteArrayOutputStream()

        try {
            // Load sample invoice text from assets
            val invoiceText = context.assets.open("SAMPLE_INVOICE.txt")
                .bufferedReader(Charset.forName("UTF-8"))
                .use { it.readText() }

            // CPCL Header - Initialize label
            // ! U1 = units (dots), 200 = DPI, heat setting, height, quantity
            output.write("! U1 SETLP 7 2 46\r\n".toByteArray())

            // Set code page for Arabic (Windows-1256 = code page 28)
            output.write("! U1 ENCODING UTF-8\r\n".toByteArray())

            // Print the invoice text
            // CPCL uses TEXT command: TEXT font size x y text
            val lines = invoiceText.split("\n")
            var yPosition = 10

            for (line in lines) {
                if (line.isNotBlank()) {
                    // TEXT font size x-pos y-pos text
                    output.write("TEXT 0 0 10 $yPosition $line\r\n".toByteArray(Charset.forName("UTF-8")))
                    yPosition += 20 // Move to next line
                }
            }

            // Print and form feed
            output.write("PRINT\r\n".toByteArray())

        } catch (e: Exception) {
            // Fallback to error message
            output.write("! U1 SETLP 7 2 46\r\n".toByteArray())
            output.write("TEXT 0 0 10 10 Error: ${e.message}\r\n".toByteArray())
            output.write("PRINT\r\n".toByteArray())
        }

        return output.toByteArray()
    }

    /**
     * STEP 1: Simple CPCL Arabic test
     */
    fun step1_testSimpleArabicCPCL(context: Context): ByteArray {
        val output = ByteArrayOutputStream()

        // CPCL Header
        output.write("! U1 SETLP 7 2 46\r\n".toByteArray())

        output.write("TEXT 0 0 10 10 STEP 1: CPCL Arabic Test\r\n".toByteArray())

        // Test Arabic text
        val arabicText = "مرحبا"

        // Try with UTF-8
        output.write("TEXT 0 0 10 40 UTF-8: $arabicText\r\n".toByteArray(Charset.forName("UTF-8")))

        // Try with Windows-1256
        output.write("TEXT 0 0 10 70 Win-1256: ".toByteArray())
        output.write(arabicText.toByteArray(Charset.forName("windows-1256")))
        output.write("\r\n".toByteArray())

        // Try with ISO-8859-6
        output.write("TEXT 0 0 10 100 ISO-8859-6: ".toByteArray())
        output.write(arabicText.toByteArray(Charset.forName("ISO-8859-6")))
        output.write("\r\n".toByteArray())

        output.write("PRINT\r\n".toByteArray())

        return output.toByteArray()
    }

    /**
     * STEP 2: Test different CPCL encoding methods
     */
    fun step2_testCPCLEncodings(context: Context): ByteArray {
        val output = ByteArrayOutputStream()

        val arabicText = "مرحبا العربية"

        // CPCL Header
        output.write("! U1 SETLP 7 2 46\r\n".toByteArray())

        output.write("TEXT 0 0 10 10 STEP 2: CPCL Encoding Tests\r\n".toByteArray())

        var yPos = 40

        // Test 1: UTF-8
        output.write("TEXT 0 0 10 $yPos Test 1 UTF-8:\r\n".toByteArray())
        yPos += 20
        output.write("TEXT 0 0 10 $yPos $arabicText\r\n".toByteArray(Charset.forName("UTF-8")))
        yPos += 30

        // Test 2: Windows-1256
        output.write("TEXT 0 0 10 $yPos Test 2 Win-1256:\r\n".toByteArray())
        yPos += 20
        output.write("TEXT 0 0 10 $yPos ".toByteArray())
        output.write(arabicText.toByteArray(Charset.forName("windows-1256")))
        output.write("\r\n".toByteArray())
        yPos += 30

        // Test 3: ISO-8859-6
        output.write("TEXT 0 0 10 $yPos Test 3 ISO-8859-6:\r\n".toByteArray())
        yPos += 20
        output.write("TEXT 0 0 10 $yPos ".toByteArray())
        output.write(arabicText.toByteArray(Charset.forName("ISO-8859-6")))
        output.write("\r\n".toByteArray())

        output.write("PRINT\r\n".toByteArray())

        return output.toByteArray()
    }

    /**
     * STEP 3: Test CPCL with different font sizes and styles
     */
    fun step3_testCPCLFonts(context: Context): ByteArray {
        val output = ByteArrayOutputStream()

        val arabicText = "مرحبا"

        // CPCL Header
        output.write("! U1 SETLP 7 2 46\r\n".toByteArray())

        output.write("TEXT 0 0 10 10 STEP 3: Font Tests\r\n".toByteArray())

        var yPos = 40

        // Font 0 (default)
        output.write("TEXT 0 0 10 $yPos Font 0: ".toByteArray())
        output.write(arabicText.toByteArray(Charset.forName("UTF-8")))
        output.write("\r\n".toByteArray())
        yPos += 30

        // Font 4 (larger)
        output.write("TEXT 4 0 10 $yPos Font 4: ".toByteArray())
        output.write(arabicText.toByteArray(Charset.forName("UTF-8")))
        output.write("\r\n".toByteArray())
        yPos += 40

        // Font 7 (smallest)
        output.write("TEXT 7 0 10 $yPos Font 7: ".toByteArray())
        output.write(arabicText.toByteArray(Charset.forName("UTF-8")))
        output.write("\r\n".toByteArray())

        output.write("PRINT\r\n".toByteArray())

        return output.toByteArray()
    }

    /**
     * STEP 4: Print full invoice with CPCL
     */
    fun step4_testFullInvoiceCPCL(context: Context): ByteArray {
        val output = ByteArrayOutputStream()

        try {
            // Load sample invoice
            val invoiceText = context.assets.open("SAMPLE_INVOICE.txt")
                .bufferedReader(Charset.forName("UTF-8"))
                .use { it.readText() }

            // CPCL Header
            output.write("! U1 SETLP 7 2 46\r\n".toByteArray())

            output.write("TEXT 0 0 10 10 STEP 4: Full Invoice CPCL\r\n".toByteArray())
            output.write("TEXT 0 0 10 30 ----------------------------\r\n".toByteArray())

            // Print invoice with UTF-8
            val lines = invoiceText.split("\n")
            var yPos = 60

            for (line in lines) {
                if (yPos > 800) break // Prevent overflow
                output.write("TEXT 0 0 5 $yPos $line\r\n".toByteArray(Charset.forName("UTF-8")))
                yPos += 15
            }

            output.write("PRINT\r\n".toByteArray())

        } catch (e: Exception) {
            output.write("! U1 SETLP 7 2 46\r\n".toByteArray())
            output.write("TEXT 0 0 10 10 Error: ${e.message}\r\n".toByteArray())
            output.write("PRINT\r\n".toByteArray())
        }

        return output.toByteArray()
    }

    /**
     * Generate invoice from order using CPCL
     */
    fun generateInvoiceFromOrder(order: CreateOrderResponse): ByteArray {
        val output = ByteArrayOutputStream()

        // CPCL Header
        output.write("! U1 SETLP 7 2 46\r\n".toByteArray())

        var yPos = 10

        // Header
        output.write("TEXT 4 0 50 $yPos INVOICE\r\n".toByteArray())
        yPos += 30

        output.write("TEXT 0 0 10 $yPos Cash Van System\r\n".toByteArray())
        yPos += 20

        output.write("TEXT 0 0 10 $yPos --------------------------------\r\n".toByteArray())
        yPos += 20

        // Invoice details
        output.write("TEXT 0 0 10 $yPos Invoice: ${order.formatted_code}\r\n".toByteArray())
        yPos += 20

        output.write("TEXT 0 0 10 $yPos Order ID: ${order.id}\r\n".toByteArray())
        yPos += 20

        output.write("TEXT 0 0 10 $yPos Merchant: ${order.merchant_id}\r\n".toByteArray())
        yPos += 20

        output.write("TEXT 0 0 10 $yPos --------------------------------\r\n".toByteArray())
        yPos += 20

        output.write("TEXT 0 0 10 $yPos Total Qty: ${order.total_sold_quantity}\r\n".toByteArray())
        yPos += 20

        output.write("TEXT 0 0 10 $yPos TOTAL: ${order.total_income} EGP\r\n".toByteArray())
        yPos += 30

        output.write("TEXT 0 0 20 $yPos Thank you!\r\n".toByteArray())

        output.write("PRINT\r\n".toByteArray())

        return output.toByteArray()
    }
}
