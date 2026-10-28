package com.illa.cashvan.sales.domain.nearby

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

// Ported from the PWA's src/lib/locationStorage.test.ts. localStorage is not
// ported: writes/reads go through an in-memory map with the same key and the
// serialize/parse halves ("storage throws" has no Kotlin counterpart).
class LocationStorageTest {

    private val store = HashMap<String, String>()
    private val now = 1_700_000_000_000L

    private fun write(location: StoredAgentLocation) {
        serializeStoredLocation(location)?.let { store[LAST_KNOWN_LOCATION_STORAGE_KEY] = it }
    }

    private fun read(nowMillis: Long = now) = parseStoredLocation(store[LAST_KNOWN_LOCATION_STORAGE_KEY], nowMillis)

    @Test
    fun `returns a valid stored GPS location within the bootstrap max age`() {
        val location = StoredAgentLocation(
            latitude = 30.0444,
            longitude = 31.2357,
            timestamp = now - 60_000,
            source = LocationSource.GPS,
            accuracy = 18.0,
        )

        write(location)

        assertEquals(location, read())
        assertNotNull(store[LAST_KNOWN_LOCATION_STORAGE_KEY])
    }

    @Test
    fun `returns null for expired stored coordinates`() {
        store[LAST_KNOWN_LOCATION_STORAGE_KEY] =
            """{"latitude":30.0444,"longitude":31.2357,"timestamp":${now - 25 * 60 * 60 * 1000},"source":"gps"}"""

        assertNull(read())
    }

    @Test
    fun `returns null for malformed or non-finite stored coordinates`() {
        store[LAST_KNOWN_LOCATION_STORAGE_KEY] = "{bad json"
        assertNull(read())

        // JSON.stringify writes NaN as null.
        store[LAST_KNOWN_LOCATION_STORAGE_KEY] = """{"latitude":null,"longitude":31.2357,"timestamp":$now,"source":"gps"}"""
        assertNull(read())
        assertNull(serializeStoredLocation(StoredAgentLocation(Double.NaN, 31.2357, now, LocationSource.GPS)))
    }

    @Test
    fun `does not overwrite a stored GPS fix with an IP fallback location`() {
        val gpsJson = """{"latitude":30.0444,"longitude":31.2357,"timestamp":1700000000000,"source":"gps"}"""
        store[LAST_KNOWN_LOCATION_STORAGE_KEY] = gpsJson

        write(StoredAgentLocation(latitude = 29.987, longitude = 31.2118, timestamp = 1_700_000_060_000, source = LocationSource.IP))

        assertEquals(gpsJson, store[LAST_KNOWN_LOCATION_STORAGE_KEY])
    }

    @Test
    fun `writes the JSON the PWA writes and drops a non-finite accuracy (extra)`() {
        assertEquals(
            """{"latitude":30.0444,"longitude":31,"timestamp":1700000000000,"source":"gps","accuracy":18}""",
            serializeStoredLocation(StoredAgentLocation(30.0444, 31.0, now, LocationSource.GPS, accuracy = 18.0)),
        )
        assertEquals(
            """{"latitude":30.0444,"longitude":31,"timestamp":1700000000000,"source":"gps"}""",
            serializeStoredLocation(StoredAgentLocation(30.0444, 31.0, now, LocationSource.GPS, accuracy = Double.NaN)),
        )
    }

    @Test
    fun `ignores IP-sourced, unknown-source and non-number accuracy values when reading (extra)`() {
        store[LAST_KNOWN_LOCATION_STORAGE_KEY] = """{"latitude":30,"longitude":31,"timestamp":$now,"source":"ip"}"""
        assertNull(read())

        store[LAST_KNOWN_LOCATION_STORAGE_KEY] = """{"latitude":30,"longitude":31,"timestamp":$now,"source":"wifi"}"""
        assertNull(read())

        store[LAST_KNOWN_LOCATION_STORAGE_KEY] = """{"latitude":30,"longitude":31,"timestamp":$now,"source":"gps","accuracy":"18"}"""
        assertEquals(StoredAgentLocation(30.0, 31.0, now, LocationSource.GPS, accuracy = null), read())
    }

    @Test
    fun `treats exactly 24 hours as still usable (extra)`() {
        val location = StoredAgentLocation(30.0, 31.0, now, LocationSource.GPS)

        assertEquals(true, isStoredLocationUsable(location, now + STORED_LOCATION_MAX_AGE_MS))
        assertEquals(false, isStoredLocationUsable(location, now + STORED_LOCATION_MAX_AGE_MS + 1))
    }
}
