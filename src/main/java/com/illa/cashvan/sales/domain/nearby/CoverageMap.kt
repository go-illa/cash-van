package com.illa.cashvan.sales.domain.nearby

import com.illa.cashvan.sales.domain.kpi.DateRange
import com.illa.cashvan.sales.domain.kpi.currentCairoPeriod
import com.illa.cashvan.sales.domain.kpi.monthBounds
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Instant

// Pure parts of the PWA's src/hooks/useCoverageMapMerchants.ts: PostgREST
// pagination/chunking helpers (the query itself is injected), visit counting
// and marker assembly. The Supabase queries are not ported.

/** Rows requested per page (Supabase's default row cap). */
const val COVERAGE_PAGE_SIZE = 1000

/** IDs per `in(...)` filter. */
const val COVERAGE_ID_CHUNK_SIZE = 200

private const val MAX_PAGES = 1000

// Hard-coded in the PWA (not VISITS_PER_MERCHANT_PER_MONTH).
private const val COVERAGE_VISIT_CAP = 2

/** One PostgREST page: rows, or the error message that failed it. */
data class PageResult<T>(val data: List<T>?, val errorMessage: String? = null)

/**
 * Pages a deterministically ordered range query until an empty page. Advances
 * by the rows actually returned, so it stays correct when the server's
 * max_rows is below [COVERAGE_PAGE_SIZE]. A page error throws (never partial
 * results); so does exceeding the 1000-page safety limit.
 */
suspend fun <T> fetchAllRows(query: suspend (from: Int, to: Int) -> PageResult<T>): List<T> {
    val results = mutableListOf<T>()
    var from = 0
    repeat(MAX_PAGES) {
        val page = query(from, from + COVERAGE_PAGE_SIZE - 1)
        page.errorMessage?.let { throw IllegalStateException(it) }
        val rows = page.data.orEmpty()
        if (rows.isEmpty()) return results
        results += rows
        from += rows.size
    }
    throw IllegalStateException("Pagination exceeded the $MAX_PAGES-page safety limit")
}

private fun List<String>.idChunks(): List<List<String>> = chunked(COVERAGE_ID_CHUNK_SIZE)

/** Runs [fetcher] per ID chunk in parallel; results keep chunk order. */
suspend fun <T> fetchInChunks(ids: List<String>, fetcher: suspend (chunk: List<String>) -> List<T>): List<T> =
    coroutineScope {
        ids.idChunks().map { chunk -> async { fetcher(chunk) } }.awaitAll().flatten()
    }

/** [fetchInChunks] for chunks whose own result set can exceed the row cap: each chunk is paginated. */
suspend fun <T> fetchInChunksPaginated(
    ids: List<String>,
    fetcher: suspend (chunk: List<String>, from: Int, to: Int) -> PageResult<T>,
): List<T> = fetchInChunks(ids) { chunk -> fetchAllRows { from, to -> fetcher(chunk, from, to) } }

/** `visits` row needed for coverage: merchant + ad-hoc flag. */
data class CoverageVisitRow(val merchantId: String, val isAdHoc: Boolean?)

/** Non-ad-hoc visits per merchant (null counts as regular), capped at 2. */
fun countCoverageVisits(visits: List<CoverageVisitRow>): Map<String, Int> {
    val visitCount = LinkedHashMap<String, Int>()
    for (visit in visits) {
        if (visit.isAdHoc == true) continue
        visitCount[visit.merchantId] = minOf((visitCount[visit.merchantId] ?: 0) + 1, COVERAGE_VISIT_CAP)
    }
    return visitCount
}

/** Presell + linked-cashvan route merchants, deduplicated in first-seen order. */
fun mergeCoverageMerchantIds(presellIds: List<String>, cashvanIds: List<String>): List<String> =
    (presellIds + cashvanIds).distinct()

/** `[start, end)` of the current Cairo month for the visits query. */
fun coverageVisitsWindow(now: Instant = Instant.now()): DateRange {
    val period = currentCairoPeriod(now)
    return monthBounds(period.month, period.year)
}

/** `merchants` row as selected for the map (channel = `merchant_types.name`). */
data class CoverageMerchantRow(
    val id: String,
    val name: String?,
    val signName: String?,
    val merchantCode: String?,
    val phoneNumber: String?,
    val latitude: Double?,
    val longitude: Double?,
    val googleLink: String?,
    val priceTier: String?,
    val channel: String?,
    val fawryDeviceNumber: String?,
    val wantsFawryDevice: Boolean?,
)

data class CoverageMapMerchant(
    val id: String,
    val name: String,
    val signName: String?,
    val merchantCode: String?,
    val phoneNumber: String?,
    val latitude: Double,
    val longitude: Double,
    val googleLink: String?,
    /** 0, 1 or 2. */
    val visitCount: Int,
    val priceTier: String?,
    val channel: String?,
    val fawryDeviceNumber: String?,
    val wantsFawryDevice: Boolean?,
)

/** Merchants without finite coordinates get no marker (and are not queried for visits). */
fun hasCoverageCoordinates(row: CoverageMerchantRow): Boolean =
    row.latitude?.isFinite() == true && row.longitude?.isFinite() == true

/** Map markers for the merchants with coordinates, in row order. */
fun buildCoverageMapMerchants(
    rows: List<CoverageMerchantRow>,
    visitCounts: Map<String, Int>,
): List<CoverageMapMerchant> = rows.mapNotNull { row ->
    val latitude = row.latitude?.takeIf { it.isFinite() } ?: return@mapNotNull null
    val longitude = row.longitude?.takeIf { it.isFinite() } ?: return@mapNotNull null
    CoverageMapMerchant(
        id = row.id,
        name = row.name ?: "",
        signName = row.signName,
        merchantCode = row.merchantCode,
        phoneNumber = row.phoneNumber,
        latitude = latitude,
        longitude = longitude,
        googleLink = row.googleLink,
        visitCount = visitCounts[row.id] ?: 0,
        priceTier = row.priceTier,
        channel = row.channel,
        fawryDeviceNumber = row.fawryDeviceNumber,
        wantsFawryDevice = row.wantsFawryDevice,
    )
}
