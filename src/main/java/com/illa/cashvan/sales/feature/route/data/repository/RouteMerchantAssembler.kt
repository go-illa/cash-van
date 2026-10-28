package com.illa.cashvan.sales.feature.route.data.repository

import com.illa.cashvan.sales.data.parseTimestamp
import com.illa.cashvan.sales.domain.model.RouteMerchant
import com.illa.cashvan.sales.domain.model.VISITS_PER_MERCHANT_PER_MONTH
import com.illa.cashvan.sales.feature.route.data.model.MerchantRouteRow
import com.illa.cashvan.sales.feature.route.data.model.MonthlyVisitRow
import com.illa.cashvan.sales.feature.route.data.model.RouteMerchantRow
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Combines merchants with this month's visits, as `useAgentRoute` does. Ad-hoc visits don't count towards
 * the card's progress.
 */
internal fun assembleRouteMerchants(
    merchants: List<RouteMerchantRow>,
    merchantRoutes: List<MerchantRouteRow>,
    routeNames: Map<String, String>,
    visits: List<MonthlyVisitRow>
): List<RouteMerchant> {
    val visitsByMerchant = visits.filter { it.is_ad_hoc != true }.groupBy { it.merchant_id }
    val routeByMerchant = merchantRoutes.associate { it.merchant_id to it.route_id }

    return merchants.map { merchant ->
        val merchantVisits = visitsByMerchant[merchant.id].orEmpty()
        val routeId = routeByMerchant[merchant.id].orEmpty()
        RouteMerchant(
            merchantId = merchant.id,
            merchantCode = merchant.merchant_code.orEmpty(),
            merchantName = merchant.name.orEmpty(),
            signName = merchant.sign_name,
            latitude = merchant.latitude,
            longitude = merchant.longitude,
            location = merchant.location,
            priceTier = merchant.price_tier,
            phoneNumber = merchant.phone_number,
            secondaryPhone = merchant.secondary_phone,
            detailedAddress = merchant.detailed_address,
            governorate = merchant.governorate,
            frontdoorCode = merchant.frontdoor_code,
            googleLink = merchant.google_link,
            workingDays = merchant.working_days.orEmpty()
                .filterValues { (it as? JsonPrimitive)?.let { value -> value.booleanOrNull ?: (value.content == "true") } == true }
                .keys.toList(),
            regionId = merchant.region_id,
            fawryDeviceNumber = merchant.fawry_device_number,
            wantsFawryDevice = merchant.wants_fawry_device,
            routeId = routeId,
            routeName = routeNames[routeId].orEmpty(),
            visitCount = minOf(merchantVisits.size, VISITS_PER_MERCHANT_PER_MONTH),
            lastVisitDate = merchantVisits.maxByOrNull { parseTimestamp(it.visit_date) ?: java.time.Instant.MIN }?.visit_date,
            hasOrder = merchantVisits.any { it.visit_type == VISIT_WITH_ORDER },
            targetCompleted = merchantVisits.size >= VISITS_PER_MERCHANT_PER_MONTH
        )
    }
}

internal const val VISIT_WITH_ORDER = "with_order"
