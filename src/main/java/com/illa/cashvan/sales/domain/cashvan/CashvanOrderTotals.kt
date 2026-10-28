package com.illa.cashvan.sales.domain.cashvan

import com.illa.cashvan.sales.domain.format.jsTrim

/** The value itself when finite, else null. */
fun finiteNumberOrNull(value: Double?): Double? = value?.takeIf { it.isFinite() }

/**
 * A serialized Rails number: null for null/blank strings, otherwise JS
 * `Number(value)` when finite (" 12 " → 12, "invalid" → null).
 */
fun finiteNumberOrNull(value: String?): Double? {
    if (value == null || value.jsTrim().isEmpty()) return null
    return finiteNumberOrNull(jsStringToNumber(value))
}

fun finiteNumberOrZero(value: Double?): Double = finiteNumberOrNull(value) ?: 0.0

fun finiteNumberOrZero(value: String?): Double = finiteNumberOrNull(value) ?: 0.0

/**
 * The order's serialized total when finite, otherwise the sum of its finite
 * line-item totals. Never concatenates (the bug string totals caused in JS).
 */
fun cashvanOrderTotal(totalIncome: Double?, lineItemTotals: List<Double?>?): Double {
    finiteNumberOrNull(totalIncome)?.let { return it }
    return (lineItemTotals ?: emptyList()).fold(0.0) { sum, item -> sum + finiteNumberOrZero(item) }
}

fun cashvanOrderTotal(order: RailsOrder): Double =
    cashvanOrderTotal(order.totalIncome, order.orderPlanProducts?.map { it.totalIncome })
