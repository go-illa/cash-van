package com.illa.cashvan.core.supabase

import com.illa.cashvan.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Thin wrapper over the `named("supabase")` Ktor client for PostgREST tables, RPCs and edge functions.
 * Requests use the public anon key, the same as the sales PWA.
 */
class SupabaseClient(
    @PublishedApi internal val httpClient: HttpClient,
    @PublishedApi internal val baseUrl: String = BuildConfig.SUPABASE_URL
) {

    suspend inline fun <reified T> select(
        table: String,
        query: PostgrestQuery.() -> Unit = {}
    ): List<T> {
        return httpClient.get("$baseUrl/rest/v1/$table") {
            applyQuery(PostgrestQuery().apply(query))
        }.bodyOrThrow()
    }

    suspend inline fun <reified T> selectFirstOrNull(
        table: String,
        query: PostgrestQuery.() -> Unit = {}
    ): T? = select<T>(table) {
        query()
        limit(1)
    }.firstOrNull()

    /** Inserts [body] (an object or a list) and returns the inserted rows. */
    suspend inline fun <reified B, reified T> insert(
        table: String,
        body: B,
        query: PostgrestQuery.() -> Unit = {}
    ): List<T> {
        return httpClient.post("$baseUrl/rest/v1/$table") {
            applyQuery(PostgrestQuery().apply(query))
            header(PREFER, RETURN_REPRESENTATION)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyOrThrow()
    }

    /** Updates the rows matched by [query] and returns them. */
    suspend inline fun <reified B, reified T> update(
        table: String,
        body: B,
        query: PostgrestQuery.() -> Unit
    ): List<T> {
        return httpClient.patch("$baseUrl/rest/v1/$table") {
            applyQuery(PostgrestQuery().apply(query))
            header(PREFER, RETURN_REPRESENTATION)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyOrThrow()
    }

    /** Deletes the rows matched by [query]. */
    suspend inline fun delete(
        table: String,
        query: PostgrestQuery.() -> Unit
    ) {
        httpClient.delete("$baseUrl/rest/v1/$table") {
            applyQuery(PostgrestQuery().apply(query))
        }.bodyOrThrow<Unit>()
    }

    suspend inline fun <reified T> rpc(
        function: String,
        params: JsonObject = JsonObject(emptyMap())
    ): T {
        return httpClient.post("$baseUrl/rest/v1/rpc/$function") {
            contentType(ContentType.Application.Json)
            setBody(params)
        }.bodyOrThrow()
    }

    suspend inline fun <reified B, reified T> invokeFunction(name: String, body: B): T {
        return httpClient.post("$baseUrl/functions/v1/$name") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyOrThrow()
    }

    @PublishedApi
    internal fun HttpRequestBuilder.applyQuery(query: PostgrestQuery) {
        query.build().forEach { (key, value) -> url.parameters.append(key, value) }
    }

    @PublishedApi
    internal suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
        if (!status.isSuccess()) throw toSupabaseException()
        return body()
    }

    @PublishedApi
    internal suspend fun HttpResponse.toSupabaseException(): SupabaseException {
        val raw = runCatching { bodyAsText() }.getOrDefault("")
        val json = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull()
        fun field(name: String) = runCatching { json?.get(name)?.jsonPrimitive?.contentOrNull }.getOrNull()
        return SupabaseException(
            status = status.value,
            code = field("code"),
            message = field("message") ?: field("error") ?: raw.ifBlank { status.description },
            details = field("details"),
            hint = field("hint")
        )
    }

    companion object {
        @PublishedApi internal const val PREFER = "Prefer"
        @PublishedApi internal const val RETURN_REPRESENTATION = "return=representation"
    }
}

/**
 * A failed Supabase call. For RPCs that `RAISE EXCEPTION 'DUPLICATE_VISIT'`, [message] holds that code.
 */
class SupabaseException(
    val status: Int,
    val code: String?,
    override val message: String,
    val details: String?,
    val hint: String?
) : Exception(message)
