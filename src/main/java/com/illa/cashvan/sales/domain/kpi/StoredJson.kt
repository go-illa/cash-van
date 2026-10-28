package com.illa.cashvan.sales.domain.kpi

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.abs
import kotlin.math.floor

// JSON helpers for records the PWA keeps in localStorage (JSON.parse /
// JSON.stringify semantics). kotlinx accepts unquoted literals such as `NaN` or
// `1d` that JSON.parse rejects, so number tokens are checked against the JSON grammar.

private val JSON_NUMBER = Regex("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?")
private const val MAX_SAFE_INTEGER = 9_007_199_254_740_991.0

/** The stored JSON object, or null for empty, malformed or non-object input. */
internal fun parseStoredJsonObject(raw: String?): JsonObject? {
    if (raw.isNullOrEmpty()) return null
    return try {
        Json.parseToJsonElement(raw) as? JsonObject
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun JsonElement?.nonStringPrimitive(): JsonPrimitive? = (this as? JsonPrimitive)?.takeIf { !it.isString }

/** A finite JSON number (`typeof v === 'number' && Number.isFinite(v)`), else null. */
internal fun JsonElement?.storedFiniteNumber(): Double? =
    nonStringPrimitive()?.takeIf { JSON_NUMBER.matches(it.content) }?.doubleOrNull?.takeIf { it.isFinite() }

internal fun JsonElement?.storedBoolean(): Boolean? = nonStringPrimitive()?.booleanOrNull

internal fun JsonElement?.storedString(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

/** A number as `JSON.stringify` writes it: integral values without a fraction (98765, not 98765.0). */
internal fun storedJsonNumber(value: Double): JsonPrimitive =
    if (value == floor(value) && abs(value) <= MAX_SAFE_INTEGER) JsonPrimitive(value.toLong()) else JsonPrimitive(value)
