package com.illa.cashvan.sales.domain.route

import com.illa.cashvan.sales.domain.format.jsNumberToString
import java.net.URLEncoder

data class Coordinates(
    val latitude: Double?,
    val longitude: Double?,
)

private fun Coordinates?.validPair(): Pair<Double, Double>? {
    val latitude = this?.latitude ?: return null
    val longitude = this.longitude ?: return null
    return if (latitude.isFinite() && longitude.isFinite()) latitude to longitude else null
}

// Coordinates are rendered with JS number-to-string semantics (31.0 → "31", not "31.0").
private fun Pair<Double, Double>.toQueryValue() = "${jsNumberToString(first)},${jsNumberToString(second)}"

/**
 * URL the PWA's `openMapsLocation` opens. The caller launches it (e.g. with an
 * ACTION_VIEW intent); the coordinates are inserted unencoded, as in the PWA.
 */
fun buildGoogleMapsSearchUrl(latitude: Double, longitude: Double): String =
    "https://www.google.com/maps/search/?api=1&query=${(latitude to longitude).toQueryValue()}"

/** Driving directions to [destination], from [origin] when it is known; null without a valid destination. */
fun buildGoogleMapsDirectionsUrl(destination: Coordinates, origin: Coordinates? = null): String? {
    val destinationPair = destination.validPair() ?: return null

    val params = buildList {
        add("api" to "1")
        origin.validPair()?.let { add("origin" to it.toQueryValue()) }
        add("destination" to destinationPair.toQueryValue())
        add("travelmode" to "driving")
    }

    // URLEncoder's form encoding matches URLSearchParams.toString() ("," → %2C, space → +).
    val query = params.joinToString("&") { (key, value) ->
        "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
    }
    return "https://www.google.com/maps/dir/?$query"
}
