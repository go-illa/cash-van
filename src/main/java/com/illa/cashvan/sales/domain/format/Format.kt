package com.illa.cashvan.sales.domain.format

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

// Number formatting helpers.
//
// Project convention (DIS-367): numbers are ALWAYS rendered with Western
// (English) digits, even in the Arabic RTL UI. Use these helpers instead of an
// Arabic-Indic-digit locale so the rule lives in one place.
//
// Currency is EGP, displayed with a "ج" suffix.

/**
 * Matches `value.toLocaleString('en-US', { maximumFractionDigits })`: thousands
 * separators, Western digits, trailing fraction zeros dropped. Like ICU, it
 * rounds the shortest decimal form of the double half away from zero, so
 * 1.005 → "1.01" (not `toFixed`'s "1.00"), and negative zero keeps its sign ("-0").
 */
fun formatNumber(value: Double, maxFractionDigits: Int = 0): String {
    // Intl throws a RangeError outside 0..100.
    require(maxFractionDigits in 0..100) { "maxFractionDigits must be in 0..100" }
    if (value.isNaN()) return "NaN"

    val negative = value < 0 || (value == 0.0 && 1.0 / value < 0)
    if (value.isInfinite()) return if (negative) "-∞" else "∞"

    val rounded = BigDecimal(abs(value).toString())
        .setScale(maxFractionDigits, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
    val integerPart = rounded.substringBefore('.')
    val fractionPart = rounded.substringAfter('.', missingDelimiterValue = "")
    val grouped = integerPart.reversed().chunked(3).joinToString(",").reversed()

    return buildString {
        if (negative) append('-')
        append(grouped)
        if (fractionPart.isNotEmpty()) append('.').append(fractionPart)
    }
}

/** Format an EGP amount with thousands separators and a "ج" suffix. */
fun formatEGP(value: Double, maxFractionDigits: Int = 0): String =
    "${formatNumber(value, maxFractionDigits)} ج"

/**
 * Compact a sales amount for space-constrained KPI surfaces.
 *
 * Values below one million are expressed in thousands; values at or above one
 * million are expressed in millions. Deliberately not a locale compact notation,
 * whose rounding can promote 999K to 1M.
 */
fun formatCompactSales(value: Double): String {
    val millions = abs(value) >= 1_000_000
    val divisor = if (millions) 1_000_000.0 else 1_000.0
    val suffix = if (millions) "M" else "K"
    val maxFractionDigits = if (millions) 2 else 1
    return "${formatNumber(value / divisor, maxFractionDigits)}$suffix"
}

/** Format a percentage value (already 0–100) as an integer with a % sign. */
fun formatPercent(value: Double): String = "${jsNumberToString(jsMathRound(value))}%"
