package com.illa.cashvan.sales.domain.route

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

// Ported from the PWA's src/components/RemoteOrderBadge.test.tsx (pure-function cases),
// pinned to Cairo instead of the test runner's local timezone.
class RemoteOrderDayTest {

    private val cairo: ZoneId = ZoneId.of("Africa/Cairo")

    /** Fixed "now" — 2026-10-01 14:00 Cairo time. */
    private val now: Instant = LocalDateTime.of(2026, 10, 1, 14, 0).atZone(cairo).toInstant()

    private fun daysAgoIso(days: Long, hour: Int = 10, minute: Int = 30): String =
        LocalDateTime.of(2026, 10, 1, hour, minute).minusDays(days).atZone(cairo).toInstant().toString()

    private fun bucket(orderAt: String?) = getRemoteOrderDayBucket(orderAt, now, cairo)

    @Test
    fun `returns 0 for an order earlier today`() {
        assertEquals(0, bucket(daysAgoIso(0, 8)))
    }

    @Test
    fun `counts calendar days, not 24h blocks - yesterday 23h00 is 1`() {
        assertEquals(1, bucket(daysAgoIso(1, 23)))
    }

    @Test
    fun `returns 2 and 3 for two and three calendar days ago`() {
        assertEquals(2, bucket(daysAgoIso(2)))
        assertEquals(3, bucket(daysAgoIso(3)))
    }

    @Test
    fun `returns null on the 4th day - the badge expires`() {
        assertNull(bucket(daysAgoIso(4)))
    }

    @Test
    fun `returns null for future timestamps, invalid dates, and missing input`() {
        assertNull(bucket(daysAgoIso(-1)))
        assertNull(bucket("not-a-date"))
        assertNull(bucket(null))
    }

    @Test
    fun `buckets by the Cairo calendar day for PostgREST-style timestamps`() {
        // Cairo is UTC+3 on these dates: 20:15 UTC is still Sep 30, 21:30 UTC is already Oct 1.
        assertEquals(1, bucket("2026-09-30T20:15:00.123456+00:00"))
        assertEquals(0, bucket("2026-09-30T21:30:00Z"))
        assertEquals(0, bucket("2026-09-30 21:30:00+00"))
        // No offset: read as local (Cairo) time, like JS.
        assertEquals(1, bucket("2026-09-30T23:30:00"))
    }
}
