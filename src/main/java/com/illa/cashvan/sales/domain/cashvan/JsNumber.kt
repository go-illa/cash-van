package com.illa.cashvan.sales.domain.cashvan

import com.illa.cashvan.sales.domain.format.jsTrim

private val DECIMAL_LITERAL = Regex("[+-]?(?:Infinity|(?:[0-9]+\\.?[0-9]*|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?)")
private val NON_DECIMAL_LITERAL = Regex("0([xXoObB])([0-9a-zA-Z]+)")

/**
 * JS `Number(string)`: trims JS whitespace, "" is 0, accepts `Infinity` and
 * unsigned 0x/0o/0b literals, and is NaN for anything else (so "12abc", "1d"
 * and "NaN" are all NaN, unlike Kotlin's `toDouble`).
 */
internal fun jsStringToNumber(value: String): Double {
    val text = value.jsTrim()
    if (text.isEmpty()) return 0.0

    NON_DECIMAL_LITERAL.matchEntire(text)?.let { match ->
        val radix = when (match.groupValues[1].lowercase()) {
            "x" -> 16
            "o" -> 8
            else -> 2
        }
        return match.groupValues[2].toBigIntegerOrNull(radix)?.toDouble() ?: Double.NaN
    }

    if (!DECIMAL_LITERAL.matches(text)) return Double.NaN
    return when (text) {
        "Infinity", "+Infinity" -> Double.POSITIVE_INFINITY
        "-Infinity" -> Double.NEGATIVE_INFINITY
        else -> text.toDouble()
    }
}
