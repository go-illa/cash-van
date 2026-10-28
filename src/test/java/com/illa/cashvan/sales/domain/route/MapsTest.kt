package com.illa.cashvan.sales.domain.route

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapsTest {

    @Test
    fun `builds a Google Maps driving directions URL to merchant coordinates`() {
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=30.0444%2C31.2357&travelmode=driving",
            buildGoogleMapsDirectionsUrl(destination = Coordinates(30.0444, 31.2357)),
        )
    }

    @Test
    fun `includes the agent location as origin when available`() {
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&origin=29.9792%2C31.1342&destination=30.0444%2C31.2357&travelmode=driving",
            buildGoogleMapsDirectionsUrl(
                destination = Coordinates(30.0444, 31.2357),
                origin = Coordinates(29.9792, 31.1342),
            ),
        )
    }

    @Test
    fun `returns null when destination coordinates are missing`() {
        assertNull(buildGoogleMapsDirectionsUrl(destination = Coordinates(null, 31.2357)))
    }

    @Test
    fun `renders whole-number coordinates like JS without a trailing zero`() {
        assertEquals(
            "https://www.google.com/maps/search/?api=1&query=30,-31.5",
            buildGoogleMapsSearchUrl(30.0, -31.5),
        )
    }
}
