package com.illa.cashvan.sales.domain.nearby

import com.illa.cashvan.sales.domain.kpi.DateRange
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

// Ported from the PWA's src/hooks/useCoverageMapMerchants.test.ts, plus extra
// cases for the marker assembly the hook does inline.
class CoverageMapTest {

    @Test
    fun `continues until an empty page and advances by the rows the server actually returns`() {
        val source = List(1_205) { it }
        val ranges = mutableListOf<Pair<Int, Int>>()

        val result = runBlocking {
            fetchAllRows { from, to ->
                ranges += from to to
                // A project whose PostgREST max_rows is only 400 although the client asks for 1000.
                PageResult(source.subList(minOf(from, source.size), minOf(from + 400, to + 1, source.size)))
            }
        }

        assertEquals(source, result)
        assertEquals(
            listOf(0 to 999, 400 to 1_399, 800 to 1_799, 1_200 to 2_199, 1_205 to 2_204),
            ranges,
        )
    }

    @Test
    fun `paginates every merchant-id chunk independently`() {
        val ids = List(201) { "merchant-$it" }
        val firstChunkRows = List(1_001) { "visit-$it" }
        val secondChunkRows = listOf("visit-last")

        val result = runBlocking {
            fetchInChunksPaginated(ids) { chunk, from, to ->
                val rows = if (chunk[0] == "merchant-0") firstChunkRows else secondChunkRows
                PageResult(rows.subList(minOf(from, rows.size), minOf(to + 1, rows.size)))
            }
        }

        assertEquals(1_002, result.size)
        assertEquals("visit-last", result.last())
    }

    @Test
    fun `surfaces a page error instead of returning partial visit counts`() {
        val error = assertThrows(IllegalStateException::class.java) {
            runBlocking { fetchAllRows<Int> { _, _ -> PageResult(null, errorMessage = "query failed") } }
        }

        assertEquals("query failed", error.message)
    }

    @Test
    fun `preserves coverage business rules after rows from all pages are merged`() {
        val counts = countCoverageVisits(
            listOf(
                CoverageVisitRow("merchant-a", isAdHoc = false),
                CoverageVisitRow("merchant-a", isAdHoc = null),
                CoverageVisitRow("merchant-a", isAdHoc = false),
                CoverageVisitRow("merchant-b", isAdHoc = true),
                CoverageVisitRow("merchant-b", isAdHoc = false),
            ),
        )

        assertEquals(mapOf("merchant-a" to 2, "merchant-b" to 1), counts)
    }

    @Test
    fun `fails after the page safety limit instead of looping forever (extra)`() {
        val error = assertThrows(IllegalStateException::class.java) {
            runBlocking { fetchAllRows { from, _ -> PageResult(listOf(from)) } }
        }

        assertEquals("Pagination exceeded the 1000-page safety limit", error.message)
    }

    @Test
    fun `splits ids into chunks of 200 and keeps chunk order (extra)`() {
        val ids = List(450) { "m-$it" }
        val chunkSizes = mutableListOf<Int>()

        val result = runBlocking {
            fetchInChunks(ids) { chunk ->
                synchronized(chunkSizes) { chunkSizes += chunk.size }
                listOf(chunk.first())
            }
        }

        assertEquals(listOf("m-0", "m-200", "m-400"), result)
        assertEquals(listOf(50, 200, 200), chunkSizes.sorted())
        assertEquals(emptyList<String>(), runBlocking { fetchInChunks(emptyList()) { listOf(it) } })
    }

    @Test
    fun `merges presell and cashvan merchant ids without duplicates (extra)`() {
        assertEquals(listOf("a", "b", "c"), mergeCoverageMerchantIds(listOf("a", "b"), listOf("b", "c", "a")))
    }

    @Test
    fun `builds markers only for merchants with finite coordinates (extra)`() {
        fun merchant(id: String, latitude: Double?, longitude: Double?, name: String? = "Shop $id") = CoverageMerchantRow(
            id = id,
            name = name,
            signName = null,
            merchantCode = "C-$id",
            phoneNumber = null,
            latitude = latitude,
            longitude = longitude,
            googleLink = null,
            priceTier = "retail",
            channel = "Grocery",
            fawryDeviceNumber = null,
            wantsFawryDevice = null,
        )
        val rows = listOf(
            merchant("a", 30.0, 31.0),
            merchant("b", null, 31.0),
            merchant("c", Double.NaN, 31.0),
            merchant("d", 30.1, 31.1, name = null),
        )

        val markers = buildCoverageMapMerchants(rows, mapOf("a" to 2, "b" to 1))

        assertEquals(listOf("a", "d"), markers.map { it.id })
        assertEquals(listOf(2, 0), markers.map { it.visitCount })
        assertEquals("", markers[1].name)
        assertEquals("Grocery", markers[0].channel)
        assertEquals(listOf(true, false, false, true), rows.map(::hasCoverageCoordinates))
    }

    @Test
    fun `queries visits within the current Cairo month (extra)`() {
        assertEquals(
            DateRange("2026-09-30T21:00:00.000Z", "2026-10-31T22:00:00.000Z"),
            coverageVisitsWindow(Instant.parse("2026-09-30T21:30:00Z")),
        )
    }
}
