package com.illa.cashvan.sales.feature.cashvan.domain.repository

import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanOrderRow
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanPlan
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanPlanProductRow
import com.illa.cashvan.sales.feature.cashvan.data.model.FawryBadgeRow
import com.illa.cashvan.sales.feature.cashvan.data.model.NearbyMerchant
import com.illa.cashvan.sales.feature.cashvan.data.model.PlanDetailRow
import com.illa.cashvan.sales.feature.cashvan.data.model.RailsMerchantPricing
import com.illa.cashvan.sales.feature.cashvan.data.model.VisitRecommendationsRow

/** Cash van selling through the Rails API, plus the Supabase reads the PWA adds on top. */
interface CashVanRepository {
    /** Rails' nearest merchants around the agent (the server picks the radius, about 1 km). */
    suspend fun getNearbyMerchants(latitude: Double, longitude: Double): List<NearbyMerchant>

    /** Fawry badge fields from Supabase, keyed by merchant id. Empty on failure. */
    suspend fun getFawryBadges(merchantIds: List<String>): Map<String, FawryBadgeRow>

    /** The merchant's current price tier and governorate, which every cash van price depends on. */
    suspend fun getMerchantPricing(merchantId: String): RailsMerchantPricing

    /** The agent's open plan (the truck load), or null when there is none or its sales are closed. */
    suspend fun getOngoingPlan(): CashVanPlan?

    /** Every page of the plan's products with their price rows; [merchantId] lets Rails price for that merchant. */
    suspend fun getPlanProducts(planId: String, merchantId: String?): List<CashVanPlanProductRow>

    /** The plan's cash van orders, newest first. */
    suspend fun getPlanOrders(planId: String): List<CashVanOrderRow>

    /** Supabase `get_visit_recommendations` for this merchant and truck (plan). Null when it fails. */
    suspend fun getVisitRecommendations(merchantId: String, planId: String): VisitRecommendationsRow?

    /** Full plan detail for the daily reconciliation; null when the plan's sales are closed. */
    suspend fun getPlanDetail(planId: String): PlanDetailRow?

    /** Principal (vendor) name by SKU, for grouping the reconciliation. Empty on failure. */
    suspend fun getPrincipalNamesBySku(skus: List<String>): Map<String, String>

    /** @throws CashVanOrderException with the message the PWA shows for the failure. */
    suspend fun createOrder(request: CashVanOrderRequest)
}

data class CashVanOrderRequest(
    val planId: String,
    val merchantId: String,
    /** `cash`, `credit` or `fawry`. */
    val paymentType: String,
    val rebateValue: Double,
    val latitude: Double?,
    val longitude: Double?,
    /** Plan product id to quantity. */
    val items: Map<String, Int>,
    /** Fawry orders only: the receipt as a `data:` URL. */
    val fawryInvoiceDataUrl: String? = null
)

class CashVanOrderException(message: String, cause: Throwable? = null) : Exception(message, cause)
