package com.illa.cashvan.sales.domain.route

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantFiltersTest {

    private data class Merchant(override val visitCount: Int, override val targetCompleted: Boolean) :
        MerchantVisitProgress

    @Test
    fun `resolves the unvisited dashboard deep link`() {
        assertEquals(MerchantFilterStatus.UNVISITED, resolveMerchantFilterStatus("unvisited"))
    }

    @Test
    fun `defaults unsupported filter values to all`() {
        listOf(null, "", "unknown").forEach { value ->
            assertEquals("value=$value", MerchantFilterStatus.ALL, resolveMerchantFilterStatus(value))
        }
    }

    @Test
    fun `shows only merchants without a visit when unvisited is selected`() {
        assertTrue(matchesMerchantFilter(Merchant(0, false), MerchantFilterStatus.UNVISITED))
        assertFalse(matchesMerchantFilter(Merchant(1, false), MerchantFilterStatus.UNVISITED))
        assertFalse(matchesMerchantFilter(Merchant(2, true), MerchantFilterStatus.UNVISITED))
    }
}
