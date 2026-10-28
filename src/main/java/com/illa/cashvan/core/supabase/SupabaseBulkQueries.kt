package com.illa.cashvan.core.supabase

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * PostgREST returns at most 1000 rows per request; this pages through with offset/limit until a short page.
 */
suspend inline fun <reified T> SupabaseClient.selectAll(
    table: String,
    pageSize: Int = SupabaseBulk.PAGE_SIZE,
    crossinline query: PostgrestQuery.() -> Unit
): List<T> {
    val rows = mutableListOf<T>()
    var offset = 0
    repeat(SupabaseBulk.MAX_PAGES) {
        val page = select<T>(table) {
            query()
            range(offset, offset + pageSize - 1)
        }
        rows += page
        if (page.size < pageSize) return rows
        offset += pageSize
    }
    return rows
}

/**
 * Splits [ids] into chunks for an `in.(...)` filter, since the Supabase gateway caps URLs at ~8KB, and runs
 * the chunks in parallel. Each chunk is paged with [selectAll].
 */
suspend inline fun <reified T> SupabaseClient.selectInChunks(
    table: String,
    ids: Collection<String>,
    chunkSize: Int = SupabaseBulk.IN_CHUNK_SIZE,
    crossinline query: PostgrestQuery.(chunk: List<String>) -> Unit
): List<T> {
    if (ids.isEmpty()) return emptyList()
    return coroutineScope {
        ids.chunked(chunkSize)
            .map { chunk -> async { selectAll<T>(table) { query(chunk) } } }
            .awaitAll()
            .flatten()
    }
}

object SupabaseBulk {
    const val PAGE_SIZE = 1000
    const val MAX_PAGES = 1000

    /** 200 UUIDs is about 7.4KB once URL-encoded. */
    const val IN_CHUNK_SIZE = 200
}
