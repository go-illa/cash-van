package com.illa.cashvan.sales.domain.route

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// No TS test exists; these pin the JS rounding used for display.
class GeofenceTest {

    @Test
    fun `formats distances with JS Math round and toFixed semantics`() {
        assertEquals("85m", formatDistance(85.4))
        assertEquals("86m", formatDistance(85.5))
        assertEquals("1000m", formatDistance(999.6))
        assertEquals("1.2km", formatDistance(1234.0))
        // toFixed rounds the exact binary value: 1.25 is exact (rounds up), 1.15 is just below.
        assertEquals("1.3km", formatDistance(1250.0))
        assertEquals("1.1km", formatDistance(1150.0))
    }

    @Test
    fun `merchants without coordinates are never fenced`() {
        assertTrue(isWithinGeofence(30.0, 31.0, null, 31.0))
        assertNull(getDistanceToMerchant(30.0, 31.0, 30.0, null))
    }

    @Test
    fun `fences by the haversine distance`() {
        // ~0.0009 degrees of latitude is ~100 m.
        assertTrue(isWithinGeofence(30.0444, 31.2357, 30.0444 + 0.0008, 31.2357))
        assertFalse(isWithinGeofence(30.0444, 31.2357, 30.0444 + 0.0010, 31.2357))
        assertEquals(111_195.0, calculateDistance(0.0, 0.0, 1.0, 0.0), 1.0)
    }
}
