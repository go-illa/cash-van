package com.illa.cashvan.sales.domain.cart

/** JS `Number.MAX_SAFE_INTEGER` (2^53 - 1). Quantities are Long so this bound matches the PWA. */
const val MAX_SAFE_INTEGER: Long = 9_007_199_254_740_991L

// `[0-9]` + matches() mirrors JS /^\d+$/: ASCII digits only, and no trailing-newline allowance.
private val WHOLE_NUMBER = Regex("[0-9]+")

/**
 * Parses a typed quantity. Non-numeric, fractional, signed or unsafe-integer
 * input restores [currentValue]; an unchanged value is returned as-is even
 * when outside [min]..[max] (a restored excessive quantity stays until edited).
 */
fun normalizeQuantityInput(draft: String, currentValue: Long, min: Long, max: Long? = null): Long {
    if (!WHOLE_NUMBER.matches(draft)) return currentValue

    // Digit strings beyond Long range are certainly beyond MAX_SAFE_INTEGER.
    val parsed = draft.toLongOrNull() ?: return currentValue
    if (parsed > MAX_SAFE_INTEGER) return currentValue
    if (parsed == currentValue) return currentValue

    return minOf(maxOf(parsed, min), max ?: MAX_SAFE_INTEGER)
}

sealed interface QuantityCommitDecision {
    data class Apply(val value: Long) : QuantityCommitDecision

    data class ConfirmCap(val enteredValue: Long, val cappedValue: Long) : QuantityCommitDecision
}

fun getQuantityCommitDecision(
    draft: String,
    currentValue: Long,
    min: Long,
    max: Long?,
    confirmExceedingMax: Boolean,
): QuantityCommitDecision {
    val enteredValue = normalizeQuantityInput(draft, currentValue, min)

    if (confirmExceedingMax && max != null && enteredValue > max && enteredValue != currentValue) {
        return QuantityCommitDecision.ConfirmCap(enteredValue = enteredValue, cappedValue = max)
    }

    return QuantityCommitDecision.Apply(normalizeQuantityInput(draft, currentValue, min, max))
}
