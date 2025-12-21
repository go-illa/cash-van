package com.illa.cashvan.feature.printer

/**
 * Enum for supported printer command languages
 */
enum class PrinterCommandLanguage {
    ESC_POS,  // ESC/P (Epson Standard Code for Printers) - Most common for thermal printers
    CPCL,     // Common Printing Command Language
    ZPL,      // Zebra Programming Language
    TSC       // TSC command language
}

/**
 * ESC/POS Command Constants
 */
object EscPosCommands {
    // Initialize printer
    val INIT = byteArrayOf(0x1B, 0x40)

    // Text formatting
    val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    val UNDERLINE_ON = byteArrayOf(0x1B, 0x2D, 0x01)
    val UNDERLINE_OFF = byteArrayOf(0x1B, 0x2D, 0x00)
    val DOUBLE_HEIGHT_ON = byteArrayOf(0x1B, 0x21, 0x10)
    val DOUBLE_WIDTH_ON = byteArrayOf(0x1B, 0x21, 0x20)
    val NORMAL_TEXT = byteArrayOf(0x1B, 0x21, 0x00)

    // Alignment
    val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)

    // Line feed
    val LINE_FEED = byteArrayOf(0x0A)
    val FEED_PAPER_AND_CUT = byteArrayOf(0x1B, 0x69)

    // Paper cut
    val PAPER_CUT = byteArrayOf(0x1D, 0x56, 0x00)
    val PARTIAL_CUT = byteArrayOf(0x1D, 0x56, 0x01)
}
