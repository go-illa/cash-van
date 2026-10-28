package com.illa.cashvan.core.supabase

import org.junit.Assert.assertEquals
import org.junit.Test

class PostgrestQueryTest {

    private fun query(block: PostgrestQuery.() -> Unit) = PostgrestQuery().apply(block).build()

    @Test
    fun `select and filters keep their order`() {
        val params = query {
            select("id, name,\n merchant:merchants(id)")
            eq("agent_id", "a-1")
            gte("created_at", "2026-10-27")
            notNull("phone_number")
        }
        assertEquals(
            listOf(
                "select" to "id, name,merchant:merchants(id)",
                "agent_id" to "eq.a-1",
                "created_at" to "gte.2026-10-27",
                "phone_number" to "not.is.null"
            ),
            params
        )
    }

    @Test
    fun `in lists quote values with reserved characters`() {
        val params = query { isIn("id", listOf("a-1", "b,2", "2026-10-27T10:00:00")) }
        assertEquals(listOf("id" to "in.(a-1,\"b,2\",\"2026-10-27T10:00:00\")"), params)
    }

    @Test
    fun `orders are joined and range sets limit and offset`() {
        val params = query {
            order("visit_date", ascending = false)
            order("name", nullsFirst = false)
            range(1000, 1999)
        }
        assertEquals(
            listOf(
                "order" to "visit_date.desc,name.asc.nullslast",
                "limit" to "1000",
                "offset" to "1000"
            ),
            params
        )
    }

    @Test
    fun `or joins raw conditions`() {
        assertEquals(
            listOf("or" to "(status.eq.1,status.is.null)"),
            query { or("status.eq.1", "status.is.null") }
        )
    }
}
