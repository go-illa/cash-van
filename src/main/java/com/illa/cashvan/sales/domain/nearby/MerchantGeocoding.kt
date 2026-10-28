package com.illa.cashvan.sales.domain.nearby

import com.illa.cashvan.sales.domain.format.jsNumberToString
import com.illa.cashvan.sales.domain.format.jsTrim
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.URLEncoder
import java.util.concurrent.CancellationException

// Reverse geocoding for the Cashvan create-merchant form, ported from the PWA's
// src/lib/merchantGeocoding.ts: response parsing, the Nominatim URL and the
// provider fallback order. The HTTP calls are injected.

enum class GeocodeSource { RAILS, OPENSTREETMAP }

/** Address details from one provider; "" when the provider had none. */
data class MerchantGeocodeFields(val address: String, val governorateName: String)

data class MerchantGeocodeResult(val address: String, val governorateName: String, val source: GeocodeSource)

/** Timeout the PWA applies to the Nominatim request. */
const val NOMINATIM_TIMEOUT_MS = 8_000L

private const val NO_LOCATION_DETAILS = "Reverse geocoding returned no location details"

private val EMPTY_RECORD = JsonObject(emptyMap())

/** JS `typeof value === 'object' && value !== null`: arrays count as records without these keys. */
private fun asRecord(value: JsonElement?): JsonObject? = when (value) {
    is JsonObject -> value
    is JsonArray -> EMPTY_RECORD
    else -> null
}

/** First key holding a non-blank string, trimmed; "" otherwise. */
private fun readString(record: JsonObject?, keys: List<String>): String {
    if (record == null) return ""
    for (key in keys) {
        val value = (record[key] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: continue
        val trimmed = value.jsTrim()
        if (trimmed.isNotEmpty()) return trimmed
    }
    return ""
}

/** Rails `geocode/reverse`: a flat body, or a `data` envelope with nested `components` (older environments). */
fun parseRailsGeocodeResponse(payload: JsonElement?): MerchantGeocodeFields {
    val root = asRecord(payload)
    val body = asRecord(root?.get("data")) ?: root
    val components = asRecord(body?.get("components"))
    return MerchantGeocodeFields(
        address = readString(body, listOf("address", "detailed_address", "formatted_address")),
        governorateName = readString(body, listOf("governorate_name", "governorate"))
            .ifEmpty { readString(components, listOf("governorate", "state", "county")) },
    )
}

/** Nominatim `reverse?format=json&addressdetails=1`. */
fun parseNominatimGeocodeResponse(payload: JsonElement?): MerchantGeocodeFields {
    val root = asRecord(payload)
    val address = asRecord(root?.get("address"))
    return MerchantGeocodeFields(
        address = readString(root, listOf("display_name")),
        governorateName = readString(address, listOf("state", "state_district", "county", "city")),
    )
}

/** Nominatim reverse-geocode URL (URLSearchParams encoding; coordinates as JS number strings). */
fun buildNominatimReverseUrl(latitude: Double, longitude: Double): String {
    val params = listOf(
        "format" to "json",
        "lat" to jsNumberToString(latitude),
        "lon" to jsNumberToString(longitude),
        "accept-language" to "ar,en",
        "addressdetails" to "1",
    )
    // URLEncoder's form encoding matches URLSearchParams.toString() ("," → %2C, space → +).
    val query = params.joinToString("&") { (key, value) ->
        "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
    }
    return "https://nominatim.openstreetmap.org/reverse?$query"
}

private fun MerchantGeocodeFields.hasAny(): Boolean = address.isNotEmpty() || governorateName.isNotEmpty()

/**
 * OpenStreetMap first (it mirrors Presell's working path); Rails only when OSM
 * fails or returns partial data, filling the missing fields. [openStreetMap]
 * should throw on a non-OK response; [rails] returns the raw Rails payload.
 * Throws when neither provider yields any detail.
 */
suspend fun resolveCashvanMerchantGeocode(
    openStreetMap: suspend () -> MerchantGeocodeFields,
    rails: suspend () -> JsonElement?,
): MerchantGeocodeResult {
    var openStreetMapResult = MerchantGeocodeFields(address = "", governorateName = "")

    try {
        openStreetMapResult = openStreetMap()
        if (openStreetMapResult.address.isNotEmpty() && openStreetMapResult.governorateName.isNotEmpty()) {
            return openStreetMapResult.withSource(GeocodeSource.OPENSTREETMAP)
        }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        // The PWA logs a warning and falls back to Rails.
    }

    try {
        val railsResult = parseRailsGeocodeResponse(rails())
        val merged = MerchantGeocodeFields(
            address = openStreetMapResult.address.ifEmpty { railsResult.address },
            governorateName = openStreetMapResult.governorateName.ifEmpty { railsResult.governorateName },
        )
        if (!merged.hasAny()) throw IllegalStateException(NO_LOCATION_DETAILS)
        val source = if (openStreetMapResult.hasAny()) GeocodeSource.OPENSTREETMAP else GeocodeSource.RAILS
        return merged.withSource(source)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        if (openStreetMapResult.hasAny()) return openStreetMapResult.withSource(GeocodeSource.OPENSTREETMAP)
        throw error
    }
}

private fun MerchantGeocodeFields.withSource(source: GeocodeSource) = MerchantGeocodeResult(address, governorateName, source)
