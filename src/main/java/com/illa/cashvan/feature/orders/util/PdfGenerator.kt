package com.illa.cashvan.feature.orders.util

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File

object PdfGenerator {

    fun generateFromText(text: String, orderCode: String, context: Context): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 40f
            val lineHeight = 18f
            val fontSize = 11f

            val paint = Paint().apply {
                textSize = fontSize
                isAntiAlias = true
                color = android.graphics.Color.BLACK
                typeface = android.graphics.Typeface.MONOSPACE
            }

            val lines = text.lines()
            val linesPerPage = ((pageHeight - 2 * margin) / lineHeight).toInt()
            var pageNumber = 1
            var lineIndex = 0

            while (lineIndex < lines.size) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                var yPosition = margin
                val endLine = minOf(lineIndex + linesPerPage, lines.size)

                for (i in lineIndex until endLine) {
                    var line = lines[i]
                    if (line.isBlank()) {
                        yPosition += lineHeight
                        continue
                    }

                    val leadingSpaces = line.takeWhile { it == ' ' }.length
                    val trailingSpaces = line.takeLastWhile { it == ' ' }.length
                    line = line.trim()

                    val textWidth = paint.measureText(line)
                    val contentWidth = pageWidth - 2 * margin

                    val xPosition = when {
                        leadingSpaces > 5 && trailingSpaces > 5 -> margin + (contentWidth - textWidth) / 2
                        line.firstOrNull()?.let { isArabic(it) } == true -> pageWidth - margin - textWidth
                        else -> margin
                    }

                    canvas.drawText(line, xPosition, yPosition, paint)
                    yPosition += lineHeight
                }

                pdfDocument.finishPage(page)
                lineIndex = endLine
                pageNumber++
            }

            val pdfFile = File(context.cacheDir, "invoice_$orderCode.pdf")
            pdfFile.outputStream().use { pdfDocument.writeTo(it) }
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            null
        }
    }

    fun isPdfBytes(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
        bytes[0] == 0x25.toByte() &&
        bytes[1] == 0x50.toByte() &&
        bytes[2] == 0x44.toByte() &&
        bytes[3] == 0x46.toByte()

    private fun isArabic(char: Char): Boolean {
        val cp = char.code
        return cp in 0x0600..0x06FF || cp in 0x0750..0x077F ||
               cp in 0xFB50..0xFDFF || cp in 0xFE70..0xFEFF
    }
}
