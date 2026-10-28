package com.illa.cashvan.sales.feature.cashvan.data.repository

import com.illa.cashvan.core.supabase.SupabaseClient
import com.illa.cashvan.core.supabase.selectInChunks
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanOrderRow
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanOrdersResponse
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanPlan
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanPlanProductRow
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanPlanProductsResponse
import com.illa.cashvan.sales.feature.cashvan.data.model.FawryBadgeRow
import com.illa.cashvan.sales.feature.cashvan.data.model.NearbyMerchant
import com.illa.cashvan.sales.feature.cashvan.data.model.NearbyMerchantsResponse
import com.illa.cashvan.sales.feature.cashvan.data.model.RailsMerchantEnvelope
import com.illa.cashvan.sales.feature.cashvan.data.model.PlanDetailRow
import com.illa.cashvan.sales.feature.cashvan.data.model.PrincipalNameRow
import com.illa.cashvan.sales.feature.cashvan.data.model.ProductPrincipalRow
import com.illa.cashvan.sales.feature.cashvan.data.model.RailsMerchantPricing
import com.illa.cashvan.sales.feature.cashvan.data.model.VisitRecommendationsRow
import com.illa.cashvan.sales.feature.cashvan.domain.repository.CashVanOrderException
import com.illa.cashvan.sales.feature.cashvan.domain.repository.CashVanOrderRequest
import com.illa.cashvan.sales.feature.cashvan.domain.repository.CashVanRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import timber.log.Timber

class CashVanRepositoryImpl(
    private val httpClient: HttpClient,
    private val supabaseClient: SupabaseClient
) : CashVanRepository {

    override suspend fun getNearbyMerchants(latitude: Double, longitude: Double): List<NearbyMerchant> =
        httpClient.get("v2/merchants/nearest") {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
        }.body<NearbyMerchantsResponse>().merchants

    override suspend fun getFawryBadges(merchantIds: List<String>): Map<String, FawryBadgeRow> = try {
        supabaseClient.selectInChunks<FawryBadgeRow>("merchants", merchantIds) { chunk ->
            select("id,fawry_device_number,wants_fawry_device")
            isIn("id", chunk)
        }.associateBy { it.id }
    } catch (e: Exception) {
        Timber.w(e, "Fawry badge backfill failed")
        emptyMap()
    }

    override suspend fun getMerchantPricing(merchantId: String): RailsMerchantPricing =
        httpClient.get("v2/merchants/$merchantId").body<RailsMerchantEnvelope>().unwrap()

    override suspend fun getOngoingPlan(): CashVanPlan? =
        httpClient.get("v2/plans/ongoing") { parameter("include", "plan_products.product") }
            .body<CashVanPlan>()
            .takeIf { it.isOpen }

    override suspend fun getPlanProducts(planId: String, merchantId: String?): List<CashVanPlanProductRow> {
        val rows = mutableListOf<CashVanPlanProductRow>()
        val visited = mutableSetOf<String>()
        var page: String? = "1"
        while (page != null && visited.add(page)) {
            val response = httpClient.get("v2/plans/$planId/plan_products") {
                parameter("include", "product,product_prices")
                parameter("limit", PLAN_PRODUCTS_PAGE_SIZE)
                parameter("page", page)
                merchantId?.let { parameter("merchant_id", it) }
            }.body<CashVanPlanProductsResponse>()
            rows += response.plan_products
            page = (response.meta?.next as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content
        }
        // Rails has returned other plans' products for this route before; keep only this plan's.
        return rows.filter { it.plan_id == null || it.plan_id == planId }
    }

    override suspend fun getPlanOrders(planId: String): List<CashVanOrderRow> =
        httpClient.get("v2/orders") {
            parameter("f[order_type_eq]", "cash_van")
            parameter("f[plan_id_eq]", planId)
            parameter("sort", "created_at desc")
            parameter("include", "order_plan_products,order_plan_products.product,merchant")
        }.body<CashVanOrdersResponse>().data
            .filter { it.plan_id == null || it.plan_id == planId }

    override suspend fun getVisitRecommendations(merchantId: String, planId: String): VisitRecommendationsRow? = try {
        supabaseClient.rpc<VisitRecommendationsRow>(
            "get_visit_recommendations",
            buildJsonObject {
                put("p_merchant_id", merchantId)
                put("p_truck_id", planId)
            }
        )
    } catch (e: Exception) {
        Timber.w(e, "get_visit_recommendations failed")
        null
    }

    override suspend fun getPlanDetail(planId: String): PlanDetailRow? =
        httpClient.get("v2/plans/$planId") {
            parameter("include", "plan_products,plan_products.product,orders,orders.merchant,orders.sales_order")
            parameter("attributes", "plan_products_stats,orders_stats,merchants_stats")
        }.body<PlanDetailRow>().takeIf { it.sales_closed_at == null }

    override suspend fun getPrincipalNamesBySku(skus: List<String>): Map<String, String> = try {
        val products = supabaseClient.selectInChunks<ProductPrincipalRow>("products", skus.distinct()) { chunk ->
            select("sku,principal_id")
            isIn("sku", chunk)
        }
        val principalIds = products.mapNotNull { it.principal_id }.distinct()
        val names = supabaseClient.selectInChunks<PrincipalNameRow>("principals", principalIds) { chunk ->
            select("id,name")
            isIn("id", chunk)
        }.associate { it.id to it.name }
        products.mapNotNull { row -> row.sku?.let { sku -> names[row.principal_id]?.let { sku to it } } }.toMap()
    } catch (e: Exception) {
        Timber.w(e, "Principal lookup failed")
        emptyMap()
    }

    override suspend fun createOrder(request: CashVanOrderRequest) {
        val isFawry = request.paymentType == FAWRY
        val body = buildJsonObject {
            putJsonObject("order") {
                put("plan_id", request.planId)
                put("merchant_id", request.merchantId)
                // Fawry orders are sent without order_type, as the PWA does.
                if (!isFawry) put("order_type", CASH_VAN_ORDER_TYPE)
                put("payment_type", request.paymentType)
                put("rebate_value", request.rebateValue)
                request.latitude?.let { put("latitude", it) }
                request.longitude?.let { put("longitude", it) }
                if (isFawry) put("fawry_invoice", request.fawryInvoiceDataUrl)
                putJsonArray("order_items") {
                    request.items.forEach { (planProductId, quantity) ->
                        addJsonObject {
                            put("plan_product_id", planProductId)
                            put("sold_quantity", quantity)
                        }
                    }
                }
            }
        }
        try {
            httpClient.post("v2/orders") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        } catch (e: Exception) {
            throw CashVanOrderException(describeOrderError(e), e)
        }
    }

    /** The PWA's `describeOrderError` messages. */
    private fun describeOrderError(error: Exception): String {
        val status = (error as? ResponseException)?.response?.status?.value
        val message = error.message.orEmpty()
        return when {
            status == 422 && ("5 دقائق" in message || "آخر 5" in message) ->
                "الطلب السابق لم يكتمل بسبب خطأ في الشبكة، يرجى الانتظار 5 دقائق ثم المحاولة مرة أخرى"
            status == 422 && message.isNotBlank() -> message
            status == 401 -> "انتهت جلستك، يرجى إعادة تسجيل الدخول"
            status == 502 -> "الخادم لا يستجيب مؤقتاً، يرجى المحاولة بعد قليل"
            status == 503 -> "الخادم غير متاح حالياً، يرجى المحاولة بعد قليل"
            status == null && error is java.io.IOException -> "تعذر الاتصال بالخادم، تحقق من اتصالك بالإنترنت وأعد المحاولة"
            else -> "فشل إنشاء الطلب، يرجى المحاولة مرة أخرى"
        }
    }

    private companion object {
        const val PLAN_PRODUCTS_PAGE_SIZE = 100
        const val CASH_VAN_ORDER_TYPE = 0
        const val FAWRY = "fawry"
    }
}
