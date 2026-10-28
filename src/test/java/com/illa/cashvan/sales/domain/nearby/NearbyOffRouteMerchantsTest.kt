package com.illa.cashvan.sales.domain.nearby

import com.illa.cashvan.sales.domain.route.calculateDistance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported from the PWA's src/hooks/useNearbyOffRouteMerchants.test.ts, plus
// extra cases for the refinement/classification/request-status logic the hook
// runs inline.
class NearbyOffRouteMerchantsTest {

    @Test
    fun `does not automatically refresh nearby merchant data after the initial page load`() {
        assertEquals(
            NearbyQueryRefreshOptions(
                staleTimeMs = Long.MAX_VALUE,
                refetchIntervalMs = null,
                refetchOnMount = false,
                refetchOnWindowFocus = false,
                refetchOnReconnect = false,
            ),
            getNearbyQueryRefreshOptions(),
        )
    }

    @Test
    fun `rounds location coordinates in the query key to avoid GPS jitter reloads`() {
        val first = getNearbyOffRouteMerchantsQueryKey(GeoPoint(30.1234567, 31.7654321), listOf("route-b", "route-a"))
        val second = getNearbyOffRouteMerchantsQueryKey(GeoPoint(30.1234568, 31.7654322), listOf("route-a", "route-b"))

        assertEquals(first, second)
        assertEquals(NearbyQueryKey(30.12346, 31.76543, "route-a,route-b"), first)
        assertEquals(NearbyQueryKey(null, null, ""), getNearbyOffRouteMerchantsQueryKey(null, emptyList()))
    }

    // extra: pure logic run inside the query

    private val agent = GeoPoint(30.0444, 31.2357)

    private fun raw(id: String, latitude: String?, longitude: String?, signName: String? = null) = RawNearbyMerchant(
        id = id,
        name = "Name $id",
        signName = signName,
        merchantCode = "C-$id",
        latitude = latitude,
        longitude = longitude,
        location = null,
        phoneNumber = null,
    )

    @Test
    fun `pads the bounding box and widens longitude with latitude (extra)`() {
        val box = nearbyBoundingBox(agent.latitude, agent.longitude)
        val dLat = 1000 * 1.1 / 111_320

        assertEquals(agent.latitude - dLat, box.minLatitude, 0.0)
        assertEquals(agent.latitude + dLat, box.maxLatitude, 0.0)
        assertTrue(box.maxLongitude - agent.longitude > dLat)
        // The box must contain the 1 km circle on both axes.
        assertTrue(calculateDistance(agent.latitude, agent.longitude, box.maxLatitude, agent.longitude) > NEARBY_RADIUS_METERS)
        assertTrue(calculateDistance(agent.latitude, agent.longitude, agent.latitude, box.maxLongitude) > NEARBY_RADIUS_METERS)
    }

    @Test
    fun `parses raw coordinates like JS Number (extra)`() {
        assertEquals(30.05, parseNearbyCoordinate("30.05"))
        assertEquals(0.0, parseNearbyCoordinate(" "))
        assertNull(parseNearbyCoordinate(""))
        assertNull(parseNearbyCoordinate(null))
        assertNull(parseNearbyCoordinate("abc"))
    }

    @Test
    fun `keeps merchants within 1 km, nearest first, and drops missing coordinates (extra)`() {
        val within = refineNearbyMerchants(
            listOf(
                raw("far", "30.0644", "31.2357"), // ~2.2 km north
                raw("near", "30.0454", "31.2357"), // ~111 m
                raw("mid", "30.0494", "31.2357"), // ~556 m
                raw("missing", null, "31.2357"),
                raw("junk", "n/a", "31.2357"),
            ),
            agent.latitude,
            agent.longitude,
        )

        assertEquals(listOf("near", "mid"), within.map { it.merchant.id })
        assertEquals(30.0454, within[0].latitude)
        assertTrue(within.all { it.distance <= NEARBY_RADIUS_METERS })
    }

    @Test
    fun `classifies by merchant_routes membership only (extra)`() {
        val mine = setOf("route-1")

        assertEquals(MerchantClassification.UNASSIGNED, classifyNearbyMerchant(emptyList(), mine))
        assertEquals(MerchantClassification.MY_ROUTE, classifyNearbyMerchant(listOf("route-9", "route-1"), mine))
        assertEquals(MerchantClassification.OTHER_ROUTE, classifyNearbyMerchant(listOf("route-9"), mine))
    }

    @Test
    fun `surfaces a decline only when it is the merchant's newest request (extra)`() {
        val statuses = resolveReassignmentRequestStatuses(
            listOf(
                // Newest first.
                ReassignmentRequestRow("r5", "m1", "declined", "Wrong area", "2026-09-05", "whatsapp"),
                ReassignmentRequestRow("r4", "m2", "pending", null, "2026-09-04", "direct"),
                ReassignmentRequestRow("r3", "m2", "pending", null, "2026-09-03", "whatsapp"),
                ReassignmentRequestRow("r2", "m2", "declined", "Old", "2026-09-02", "whatsapp"),
                ReassignmentRequestRow("r1", "m3", "declined", null, "2026-09-01", "whatsapp"),
            ),
        )

        assertEquals(ReassignmentRequestStatus(pendingRequestId = null, lastDeclineNote = "Wrong area", lastDeclined = true), statuses["m1"])
        // Only WhatsApp requests count as pending; the older decline is superseded.
        assertEquals(ReassignmentRequestStatus(pendingRequestId = "r3", lastDeclineNote = null, lastDeclined = false), statuses["m2"])
        assertEquals(ReassignmentRequestStatus(pendingRequestId = null, lastDeclineNote = "", lastDeclined = true), statuses["m3"])
    }

    @Test
    fun `assembles the nearby list with routes, names and request status (extra)`() {
        val within = refineNearbyMerchants(
            listOf(
                raw("mid", "30.0494", "31.2357", signName = "Sign Mid"),
                raw("near", "30.0454", "31.2357", signName = ""),
                raw("solo", "30.0464", "31.2357"),
            ),
            agent.latitude,
            agent.longitude,
        )
        val merchantRoutes = listOf(
            MerchantRouteRow("near", "route-mine"),
            MerchantRouteRow("mid", "route-other"),
            MerchantRouteRow("mid", "route-unnamed"),
        )

        assertEquals(listOf("route-mine", "route-other", "route-unnamed"), involvedRouteIds(merchantRoutes))

        val result = assembleNearbyMerchants(
            within = within,
            merchantRoutes = merchantRoutes,
            routes = listOf(RouteNameRow("route-mine", "My Route"), RouteNameRow("route-other", "Other Route")),
            requests = listOf(ReassignmentRequestRow("req-1", "mid", "pending", null, "2026-09-01", "whatsapp")),
            myRouteIds = listOf("route-mine"),
        )

        assertEquals(listOf("near", "solo", "mid"), result.map { it.id })
        val (near, solo, mid) = result
        assertEquals("Name near", near.name) // empty sign name falls back to the name
        assertEquals(MerchantClassification.MY_ROUTE, near.classification)
        assertEquals(listOf("My Route"), near.routeNames)
        assertEquals(MerchantClassification.UNASSIGNED, solo.classification)
        assertEquals(emptyList<String>(), solo.routeIds)
        assertEquals("Sign Mid", mid.name)
        assertEquals(MerchantClassification.OTHER_ROUTE, mid.classification)
        assertEquals(listOf("Other Route", "route-unnamed"), mid.routeNames)
        assertEquals("req-1", mid.pendingRequestId)
        assertFalse(mid.lastDeclined)
        assertNull(near.lastDeclineNote)
    }
}
