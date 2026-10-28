package com.illa.cashvan.sales.domain.nearby

import com.illa.cashvan.sales.domain.kpi.parseStoredJsonObject
import com.illa.cashvan.sales.domain.kpi.storedFiniteNumber
import com.illa.cashvan.sales.domain.kpi.storedJsonNumber
import com.illa.cashvan.sales.domain.kpi.storedString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Last known agent location used to bootstrap location-dependent screens,
// ported from the PWA's src/lib/locationStorage.ts. The localStorage access is
// not ported; its parse/serialize halves are, so a stored value behaves the same.

const val LAST_KNOWN_LOCATION_STORAGE_KEY = "sales_agent_last_location_v1"
const val STORED_LOCATION_MAX_AGE_MS = 24L * 60 * 60 * 1000

enum class LocationSource(val wireValue: String) {
    GPS("gps"),
    IP("ip"),
    ;

    companion object {
        fun fromWire(value: String?): LocationSource? = entries.firstOrNull { it.wireValue == value }
    }
}

data class StoredAgentLocation(
    val latitude: Double,
    val longitude: Double,
    /** Epoch millis of the fix. */
    val timestamp: Long,
    val source: LocationSource,
    val accuracy: Double? = null,
)

/** A stored fix is usable for at most [STORED_LOCATION_MAX_AGE_MS]. */
fun isStoredLocationUsable(location: StoredAgentLocation, nowMillis: Long = System.currentTimeMillis()): Boolean =
    nowMillis - location.timestamp <= STORED_LOCATION_MAX_AGE_MS

/** The stored location when it is a valid, fresh GPS fix; null for missing, malformed, IP-sourced or expired values. */
fun parseStoredLocation(raw: String?, nowMillis: Long = System.currentTimeMillis()): StoredAgentLocation? {
    val json = parseStoredJsonObject(raw) ?: return null
    val latitude = json["latitude"].storedFiniteNumber() ?: return null
    val longitude = json["longitude"].storedFiniteNumber() ?: return null
    val timestamp = json["timestamp"].storedFiniteNumber() ?: return null
    val source = LocationSource.fromWire(json["source"].storedString()) ?: return null

    val location = StoredAgentLocation(
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp.toLong(),
        source = source,
        accuracy = json["accuracy"].storedFiniteNumber(),
    )
    if (location.source != LocationSource.GPS || !isStoredLocationUsable(location, nowMillis)) return null
    return location
}

/**
 * JSON to store, or null when nothing should be written: invalid coordinates,
 * or an IP fallback (it must never overwrite a stored GPS fix).
 */
fun serializeStoredLocation(location: StoredAgentLocation): String? {
    if (!location.latitude.isFinite() || !location.longitude.isFinite()) return null
    if (location.source != LocationSource.GPS) return null
    return buildJsonObject {
        put("latitude", storedJsonNumber(location.latitude))
        put("longitude", storedJsonNumber(location.longitude))
        put("timestamp", location.timestamp)
        put("source", location.source.wireValue)
        // A non-finite accuracy is dropped, like the PWA's normalizer.
        location.accuracy?.takeIf { it.isFinite() }?.let { put("accuracy", storedJsonNumber(it)) }
    }.toString()
}
