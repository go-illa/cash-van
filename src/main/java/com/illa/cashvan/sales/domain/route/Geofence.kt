package com.illa.cashvan.sales.domain.route

import com.illa.cashvan.sales.domain.format.jsMathRound
import com.illa.cashvan.sales.domain.format.jsNumberToString
import com.illa.cashvan.sales.domain.format.jsToFixed
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Geofencing utilities for location-based visit restrictions (Haversine distance).

/** Maximum allowed distance (in meters) from a merchant to start a visit. */
const val GEOFENCE_RADIUS_METERS = 100.0

private const val EARTH_RADIUS_METERS = 6_371_000.0

private fun toRadians(degrees: Double) = degrees * PI / 180

/** Haversine distance between two GPS coordinates, in meters. */
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = toRadians(lat2 - lat1)
    val dLon = toRadians(lon2 - lon1)

    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(toRadians(lat1)) * cos(toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return EARTH_RADIUS_METERS * c
}

/** True when the user is within the geofence radius; a merchant without coordinates is unrestricted. */
fun isWithinGeofence(userLat: Double, userLon: Double, merchantLat: Double?, merchantLon: Double?): Boolean {
    if (merchantLat == null || merchantLon == null) return true
    return calculateDistance(userLat, userLon, merchantLat, merchantLon) <= GEOFENCE_RADIUS_METERS
}

/** Distance in meters between user and merchant, or null if the merchant has no coordinates. */
fun getDistanceToMerchant(userLat: Double, userLon: Double, merchantLat: Double?, merchantLon: Double?): Double? {
    if (merchantLat == null || merchantLon == null) return null
    return calculateDistance(userLat, userLon, merchantLat, merchantLon)
}

/** "85m" below 1 km, otherwise "1.2km" (JS Math.round / toFixed(1) semantics). */
fun formatDistance(meters: Double): String {
    if (meters < 1000) return "${jsNumberToString(jsMathRound(meters))}m"
    return "${jsToFixed(meters / 1000, 1)}km"
}
