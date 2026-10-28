package com.illa.cashvan.sales.domain.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class FormatTest {

    @Test
    fun `formats compact sales amounts`() {
        listOf(
            950_000.0 to "950K",
            950_500.0 to "950.5K",
            1_000_000.0 to "1M",
            1_590_700.0 to "1.59M",
            2_500_000.0 to "2.5M",
        ).forEach { (value, expected) ->
            assertEquals("value=$value", expected, formatCompactSales(value))
        }
    }

    // Kotlin-only cases; expected strings were taken from JavaScriptCore's output.

    @Test
    fun `formatNumber matches en-US toLocaleString rounding and grouping`() {
        assertEquals("1,234,568", formatNumber(1_234_567.891))
        assertEquals("1,234,567.89", formatNumber(1_234_567.891, 2))
        assertEquals("1.01", formatNumber(1.005, 2))
        assertEquals("0.13", formatNumber(0.125, 2))
        assertEquals("3", formatNumber(2.5))
        assertEquals("-3", formatNumber(-2.5))
        assertEquals("-0", formatNumber(-0.4))
        assertEquals("∞", formatNumber(Double.POSITIVE_INFINITY))
        assertEquals("1,500 ج", formatEGP(1500.0))
    }

    @Test
    fun `formatPercent rounds like JS Math round`() {
        assertEquals("43%", formatPercent(42.5))
        assertEquals("-2%", formatPercent(-2.5))
        assertEquals("0%", formatPercent(-0.4))
    }

    @Test
    fun `JS number and toFixed helpers match the JS output`() {
        assertEquals("31", jsNumberToString(31.0))
        assertEquals("0.000001", jsNumberToString(0.000001))
        assertEquals("1e-7", jsNumberToString(1e-7))
        assertEquals("1e+21", jsNumberToString(1e21))
        assertEquals("1.5e+300", jsNumberToString(1.5e300))
        assertEquals("1.23e-18", jsNumberToString(123e-20))
        assertEquals("0.1", jsToFixed(0.05, 1))
        assertEquals("0.1", jsToFixed(0.15, 1))
        assertEquals("1.3", jsToFixed(1.25, 1))
        assertEquals("1.4", jsToFixed(1.35, 1))
    }

    @Test
    fun `parses dates like the JS Date constructor`() {
        val cairo = ZoneId.of("Africa/Cairo")
        assertEquals(1_783_944_000_000L, parseJsDateMillis("2026-07-13 12:00:00+00", cairo))
        assertEquals(1_783_944_000_123L, parseJsDateMillis("2026-07-13T12:00:00.123456+00:00", cairo))
        assertEquals(1_783_936_800_000L, parseJsDateMillis("2026-07-13T12:00:00+0200", cairo))
        assertEquals(1_783_900_800_000L, parseJsDateMillis("2026-07-13", cairo))
        assertEquals(1_783_933_200_000L, parseJsDateMillis("2026-07-13T12:00:00", cairo))
        assertEquals(1_783_933_200_000L, parseJsDateMillis("2026-07-13T12:00", cairo))
        assertEquals(1_783_944_000_000L, parseJsDateMillis("2026-07-13T12:00:00Z", cairo))
        assertNull(parseJsDateMillis("not-a-date", cairo))
    }
}
