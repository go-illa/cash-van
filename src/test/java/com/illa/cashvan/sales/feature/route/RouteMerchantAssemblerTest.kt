package com.illa.cashvan.sales.feature.route

import com.illa.cashvan.sales.domain.model.VisitProgress
import com.illa.cashvan.sales.domain.model.VisitProgressStatus
import com.illa.cashvan.sales.domain.model.visitProgress
import com.illa.cashvan.sales.feature.route.data.model.MerchantRouteRow
import com.illa.cashvan.sales.feature.route.data.model.MonthlyVisitRow
import com.illa.cashvan.sales.feature.route.data.model.RouteMerchantRow
import com.illa.cashvan.sales.feature.route.data.repository.assembleRouteMerchants
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteMerchantAssemblerTest {

    private val merchant = RouteMerchantRow(
        id = "m1",
        merchant_code = "M-1",
        name = "Ahmed Store",
        working_days = buildJsonObject {
            put("sunday", JsonPrimitive(true))
            put("monday", JsonPrimitive("true"))
            put("tuesday", JsonPrimitive(false))
        }
    )

    private fun visit(date: String, type: String = "without_order", adHoc: Boolean? = null) =
        MonthlyVisitRow(merchant_id = "m1", visit_date = date, visit_type = type, is_ad_hoc = adHoc)

    @Test
    fun `progress maps visit counts to the two-visit monthly target`() {
        assertEquals(VisitProgress(0, 2, VisitProgressStatus.UNVISITED), visitProgress(0))
        assertEquals(VisitProgress(1, 2, VisitProgressStatus.IN_PROGRESS), visitProgress(1))
        assertEquals(VisitProgress(2, 2, VisitProgressStatus.COMPLETED), visitProgress(2))
        assertEquals(VisitProgress(2, 2, VisitProgressStatus.COMPLETED), visitProgress(3))
    }

    @Test
    fun `regular visits are counted, capped and the latest date kept`() {
        val result = assembleRouteMerchants(
            merchants = listOf(merchant),
            merchantRoutes = listOf(MerchantRouteRow("m1", "r1")),
            routeNames = mapOf("r1" to "Route 1"),
            visits = listOf(
                visit("2026-10-03T08:00:00+00:00"),
                visit("2026-10-20T08:00:00+00:00", type = "with_order"),
                visit("2026-10-11T08:00:00Z"),
                visit("2026-10-25T08:00:00+00:00", type = "with_order", adHoc = true)
            )
        ).single()

        assertEquals(2, result.visitCount)
        assertTrue(result.targetCompleted)
        assertTrue(result.hasOrder)
        assertEquals("2026-10-20T08:00:00+00:00", result.lastVisitDate)
        assertEquals("Route 1", result.routeName)
        assertEquals(listOf("sunday", "monday"), result.workingDays)
    }

    @Test
    fun `ad-hoc visits alone leave the merchant unvisited`() {
        val result = assembleRouteMerchants(
            merchants = listOf(merchant),
            merchantRoutes = emptyList(),
            routeNames = emptyMap(),
            visits = listOf(visit("2026-10-25T08:00:00+00:00", type = "with_order", adHoc = true))
        ).single()

        assertEquals(0, result.visitCount)
        assertFalse(result.hasOrder)
        assertEquals("", result.routeId)
    }
}
