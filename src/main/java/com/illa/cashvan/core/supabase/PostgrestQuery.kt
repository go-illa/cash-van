package com.illa.cashvan.core.supabase

/**
 * Builds PostgREST query parameters, e.g. `select=id,name&status=eq.1&order=name.asc`.
 * See https://postgrest.org/en/stable/references/api/tables_views.html
 */
class PostgrestQuery {
    private val filters = mutableListOf<Pair<String, String>>()
    private val orders = mutableListOf<String>()
    private var columns: String? = null
    private var limit: Int? = null
    private var offset: Int? = null

    fun select(columns: String) {
        this.columns = columns.lines().joinToString("") { it.trim() }
    }

    fun eq(column: String, value: Any) = filter(column, "eq", value)
    fun neq(column: String, value: Any) = filter(column, "neq", value)
    fun gt(column: String, value: Any) = filter(column, "gt", value)
    fun gte(column: String, value: Any) = filter(column, "gte", value)
    fun lt(column: String, value: Any) = filter(column, "lt", value)
    fun lte(column: String, value: Any) = filter(column, "lte", value)

    /** `*` is the wildcard, as in PostgREST URLs. */
    fun ilike(column: String, pattern: String) = filter(column, "ilike", pattern)

    fun isNull(column: String) = filter(column, "is", "null")
    fun notNull(column: String) = filter(column, "not.is", "null")

    fun isIn(column: String, values: Collection<Any>) {
        filters += column to "in.(${values.joinToString(",") { quote(format(it)) }})"
    }

    /** Raw PostgREST conditions joined with OR, e.g. `or("status.eq.1", "status.is.null")`. */
    fun or(vararg conditions: String) {
        filters += "or" to "(${conditions.joinToString(",")})"
    }

    fun order(column: String, ascending: Boolean = true, nullsFirst: Boolean? = null) {
        orders += buildString {
            append(column)
            append(if (ascending) ".asc" else ".desc")
            nullsFirst?.let { append(if (it) ".nullsfirst" else ".nullslast") }
        }
    }

    fun limit(count: Int) {
        limit = count
    }

    fun offset(count: Int) {
        offset = count
    }

    /** Inclusive row range, like supabase-js `.range(from, to)`. */
    fun range(from: Int, to: Int) {
        offset = from
        limit = to - from + 1
    }

    fun build(): List<Pair<String, String>> = buildList {
        columns?.let { add("select" to it) }
        addAll(filters)
        if (orders.isNotEmpty()) add("order" to orders.joinToString(","))
        limit?.let { add("limit" to it.toString()) }
        offset?.let { add("offset" to it.toString()) }
    }

    private fun filter(column: String, operator: String, value: Any) {
        filters += column to "$operator.${format(value)}"
    }

    private fun format(value: Any): String = when (value) {
        is Enum<*> -> value.name
        else -> value.toString()
    }

    private fun quote(value: String): String =
        if (value.any { it in RESERVED_CHARS }) {
            "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        } else {
            value
        }

    private companion object {
        const val RESERVED_CHARS = ",.:()\" "
    }
}
