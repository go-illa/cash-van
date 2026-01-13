package com.illa.cashvan.feature.printer

import android.content.Context
import android.util.Log
import com.illa.cashvan.feature.orders.data.model.CreateOrderResponse
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * CPCL (Common Printing Command Language) invoice formatter for Honeywell printers
 * CPCL is Honeywell's preferred printing language
 */
object CpclInvoiceFormatter {

    /**
     * Format invoice as CPCL commands with proper encoding
     * CPCL commands = ASCII bytes, Text content = UTF-8 bytes
     * Optimized for 80mm thermal paper width (640 dots at 203 DPI)
     * @param invoiceText Plain text invoice content from S3
     * @return ByteArray with properly encoded CPCL commands and content
     */
    fun formatInvoiceAsCpclBytes(invoiceText: String): ByteArray {
        val lines = invoiceText.lines()
        val output = ByteArrayOutputStream()

        Log.d("CpclFormatter", "========================================")
        Log.d("CpclFormatter", "FIX v6: Multiple PRINT jobs, force separation")
        Log.d("CpclFormatter", "========================================")

        // Separate logo lines (contain █) from content lines
        val allLines = lines.map { it.trimStart() }.filter { it.isNotBlank() }
        val logoLines = allLines.filter { it.contains('█') }
        val contentLines = allLines.filter { !it.contains('█') }

        Log.d("CpclFormatter", "Logo lines: ${logoLines.size}, Content lines: ${contentLines.size}")

        // === SECTION 1: LOGO ===
        val logoHeight = (logoLines.size * 12) + 30
        output.write("! 0 200 200 $logoHeight 1\r\n".toByteArray(Charsets.US_ASCII))
        output.write("PAGE-WIDTH 640\r\n".toByteArray(Charsets.US_ASCII))
        output.write("ENCODING UTF-8\r\n".toByteArray(Charsets.US_ASCII))

        var currentY = 5
        for ((index, line) in logoLines.withIndex()) {
            // Full logo - X=0 for maximum width, no truncation
            output.write("TEXT 0 0 0 $currentY ".toByteArray(Charsets.US_ASCII))
            output.write(line.toByteArray(Charset.forName("UTF-8")))
            output.write("\r\n".toByteArray(Charsets.US_ASCII))
            Log.d("CpclFormatter", "Logo $index (y=$currentY, len=${line.length})")
            currentY += 12
        }
        output.write("FORM\r\n".toByteArray(Charsets.US_ASCII))
        output.write("PRINT\r\n".toByteArray(Charsets.US_ASCII))

        // === SECTION 2: CONTENT (each line separate label) ===
        for ((index, line) in contentLines.withIndex()) {
            // Each line gets its own mini-label
            output.write("! 0 200 200 40 1\r\n".toByteArray(Charsets.US_ASCII))
            output.write("PAGE-WIDTH 640\r\n".toByteArray(Charsets.US_ASCII))
            output.write("ENCODING UTF-8\r\n".toByteArray(Charsets.US_ASCII))
            output.write("TEXT 0 0 20 5 ".toByteArray(Charsets.US_ASCII))
            output.write(line.toByteArray(Charset.forName("UTF-8")))
            output.write("\r\n".toByteArray(Charsets.US_ASCII))
            output.write("FORM\r\n".toByteArray(Charsets.US_ASCII))
            output.write("PRINT\r\n".toByteArray(Charsets.US_ASCII))

            Log.d("CpclFormatter", "Content $index: '${line.take(40)}...'")
        }

        Log.d("CpclFormatter", "========================================")
        Log.d("CpclFormatter", "Total content lines printed: ${contentLines.size}")
        Log.d("CpclFormatter", "========================================")

        return output.toByteArray()
    }

    /**
     * SAVED WORKING STATE - Logo Font 0, Content Font 2, 35px spacing
     * Use this as fallback if new changes don't work
     */
    fun formatInvoiceAsCpclBytes_SAVED(invoiceText: String): ByteArray {
        val lines = invoiceText.lines()
        val output = ByteArrayOutputStream()

        val allLines = lines.map { it.trim() }.filter { it.isNotBlank() }
        val logoLines = allLines.filter { it.contains('█') }
        val contentLines = allLines.filter { !it.contains('█') }

        val logoLineHeight = 12
        val contentLineHeight = 35
        val labelHeight = (logoLines.size * logoLineHeight) + (contentLines.size * contentLineHeight) + 100

        output.write("! 0 200 200 $labelHeight 1\r\n".toByteArray(Charsets.US_ASCII))
        output.write("ENCODING UTF-8\r\n".toByteArray(Charsets.US_ASCII))

        var currentY = 5

        for ((index, line) in logoLines.withIndex()) {
            output.write("CENTER\r\n".toByteArray(Charsets.US_ASCII))
            output.write("TEXT 0 0 0 $currentY ".toByteArray(Charsets.US_ASCII))
            output.write(line.toByteArray(Charset.forName("UTF-8")))
            output.write("\r\n".toByteArray(Charsets.US_ASCII))
            currentY += logoLineHeight
        }

        currentY += 20

        for ((index, line) in contentLines.withIndex()) {
            output.write("CENTER\r\n".toByteArray(Charsets.US_ASCII))
            output.write("TEXT 2 0 0 $currentY ".toByteArray(Charsets.US_ASCII))
            output.write(line.toByteArray(Charset.forName("UTF-8")))
            output.write("\r\n".toByteArray(Charsets.US_ASCII))
            currentY += contentLineHeight
        }

        output.write("FORM\r\n".toByteArray(Charsets.US_ASCII))
        output.write("PRINT\r\n".toByteArray(Charsets.US_ASCII))

        return output.toByteArray()
    }

    /**
     * DEPRECATED: Old String-based formatter
     * Use formatInvoiceAsCpclBytes() instead for proper encoding
     */
    @Deprecated("Use formatInvoiceAsCpclBytes() for proper CPCL encoding")
    fun formatInvoiceTextAsCpcl(invoiceText: String): String {
        val lines = invoiceText.lines()

        Log.d("CpclFormatter", "Formatting invoice with ${lines.size} lines")

        // Calculate label height with plenty of room
        val lineSpacing = 16  // Very tight spacing for very small font
        val labelHeight = (lines.size * lineSpacing) + 100

        val cpclCommands = buildString {
            // CPCL Header - Initialize label
            // Format: ! offset horizontal-resolution vertical-resolution height quantity
            append("! 0 200 200 $labelHeight 1\r\n")

            // Set encoding for UTF-8 to support Arabic and Unicode
            append("ENCODING UTF-8\r\n")

            var yPos = 10
            for ((index, line) in lines.withIndex()) {
                // Set CENTER before EACH line for proper centering
                append("CENTER\r\n")

                // TEXT command format: TEXT font rotation magnification x-pos y-pos text
                // Font 1 is the smallest readable font - can fit 60+ chars on thermal paper
                // X position 0 when CENTER is active
                append("TEXT 1 0 0 $yPos $line\r\n")

                if (index < 10) {
                    // Log first 10 lines for debugging
                    val linePreview = if (line.length > 60) line.take(60) + "..." else line
                    Log.d("CpclFormatter", "Line $index (y=$yPos, len=${line.length}): $linePreview")
                }

                yPos += lineSpacing  // Move to next line position
            }

            // Form feed to finish printing
            append("FORM\r\n")

            // Print command - execute the print job
            append("PRINT\r\n")
        }

        Log.d("CpclFormatter", "Generated CPCL commands, length: ${cpclCommands.length}")
        Log.d("CpclFormatter", "Total lines: ${lines.size}, Label height: $labelHeight")
        Log.d("CpclFormatter", "Using Font 1 (very small) with CENTER before each line")
        Log.d("CpclFormatter", "Max line length: ${lines.maxOfOrNull { it.length } ?: 0} characters")
        Log.d("CpclFormatter", "CPCL preview:")
        Log.d("CpclFormatter", cpclCommands.take(450))

        return cpclCommands
    }

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
