package com.illa.cashvan.feature.printer

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

object CpclInvoiceFormatter {

    fun formatInvoiceAsCpclBytes(invoiceText: String): ByteArray {
        val lines = invoiceText.lines()
        val output = ByteArrayOutputStream()

        val allLines = lines.map { it.trimStart() }.filter { it.isNotBlank() }
        val logoLines = allLines.filter { it.contains('█') }
        val contentLines = allLines.filter { !it.contains('█') }

        output.write("! 0 200 200 80 1\r\n".toByteArray(Charsets.US_ASCII))
        output.write("PAGE-WIDTH 640\r\n".toByteArray(Charsets.US_ASCII))
        output.write("ENCODING UTF-8\r\n".toByteArray(Charsets.US_ASCII))
        output.write("CENTER\r\n".toByteArray(Charsets.US_ASCII))
        output.write("SETMAG 2 2\r\n".toByteArray(Charsets.US_ASCII))
        output.write("TEXT 4 0 0 10 FRONTDOOR\r\n".toByteArray(Charsets.US_ASCII))
        output.write("SETMAG 1 1\r\n".toByteArray(Charsets.US_ASCII))
        output.write("FORM\r\n".toByteArray(Charsets.US_ASCII))
        output.write("PRINT\r\n".toByteArray(Charsets.US_ASCII))

        for (line in contentLines) {
            output.write("! 0 200 200 40 1\r\n".toByteArray(Charsets.US_ASCII))
            output.write("PAGE-WIDTH 640\r\n".toByteArray(Charsets.US_ASCII))
            output.write("ENCODING UTF-8\r\n".toByteArray(Charsets.US_ASCII))
            output.write("TEXT 0 0 20 5 ".toByteArray(Charsets.US_ASCII))
            output.write(line.toByteArray(Charset.forName("UTF-8")))
            output.write("\r\n".toByteArray(Charsets.US_ASCII))
            output.write("FORM\r\n".toByteArray(Charsets.US_ASCII))
            output.write("PRINT\r\n".toByteArray(Charsets.US_ASCII))
        }

        return output.toByteArray()
    }
}
