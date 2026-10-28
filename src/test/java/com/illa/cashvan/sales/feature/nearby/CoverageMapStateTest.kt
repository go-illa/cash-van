package com.illa.cashvan.sales.feature.nearby

import com.illa.cashvan.sales.domain.model.RouteMerchant
import com.illa.cashvan.sales.feature.nearby.presentation.viewmodel.CoverageBucket
import com.illa.cashvan.sales.feature.nearby.presentation.viewmodel.CoverageMapUiState
import com.illa.cashvan.sales.feature.nearby.presentation.viewmodel.coverageBucket
import org.junit.Assert.assertEquals
import org.junit.Test

class CoverageMapStateTest {

    private fun merchant(id: String, visits: Int, name: String = "Store $id", phone: String? = null) = RouteMerchant(
        merchantId = id, merchantCode = "C$id", merchantName = name, signName = null, latitude = 30.0, longitude = 31.0,
        location = null, priceTier = null, phoneNumber = phone, secondaryPhone = null, detailedAddress = null, governorate = null,
        frontdoorCode = null, googleLink = null, workingDays = emptyList(), regionId = null, fawryDeviceNumber = null,
        wantsFawryDevice = null, routeId = "r", routeName = "R", visitCount = visits, lastVisitDate = null,
        hasOrder = false, targetCompleted = visits >= 2
    )

    private val merchants = listOf(merchant("1", 0), merchant("2", 1, phone = "01012345678"), merchant("3", 2, name = "Nile Market"))

    @Test
    fun `visits map to the three pin colours`() {
        assertEquals(listOf(CoverageBucket.UNVISITED, CoverageBucket.ONCE, CoverageBucket.COMPLETED), merchants.map { it.coverageBucket() })
    }

    @Test
    fun `legend buckets and search narrow the pins`() {
        val state = CoverageMapUiState(merchants = merchants)
        assertEquals(3, state.visible.size)
        assertEquals(listOf("1"), state.copy(buckets = setOf(CoverageBucket.UNVISITED)).visible.map { it.merchantId })
        assertEquals(listOf("3"), state.copy(search = "nile").visible.map { it.merchantId })
        assertEquals(listOf("2"), state.copy(search = "0101234").visible.map { it.merchantId })
        assertEquals(1, state.count(CoverageBucket.ONCE))
    }
}
