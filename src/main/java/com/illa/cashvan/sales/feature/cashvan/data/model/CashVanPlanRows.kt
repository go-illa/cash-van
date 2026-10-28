package com.illa.cashvan.sales.feature.cashvan.data.model

import com.illa.cashvan.core.network.util.FlexibleDoubleSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** `plans/ongoing`; `{}` when the agent has no plan. */
@Serializable
data class CashVanPlan(
    val id: String? = null,
    val code: String? = null,
    val status: String? = null,
    val plan_type: String? = null,
    val created_at: String? = null,
    val sales_closed_at: String? = null
) {
    /** A plan whose sales are closed is treated as no plan, as the PWA does. */
    val isOpen: Boolean get() = id != null && sales_closed_at == null
}

@Serializable
data class CashVanPlanProductsResponse(
    val plan_products: List<CashVanPlanProductRow> = emptyList(),
    val meta: PageMeta? = null
)

@Serializable
data class PageMeta(
    /** A page number, a string, or null on the last page. */
    val next: JsonElement? = null
)

@Serializable
data class CashVanPlanProductRow(
    val id: String,
    val plan_id: String? = null,
    val product_id: String? = null,
    val assigned_quantity: Int = 0,
    val sold_quantity: Int = 0,
    val cash_van_available_quantity: Int = 0,
    val product: CashVanProductRef? = null,
    val product_prices: List<CashVanProductPriceRow> = emptyList()
)

@Serializable
data class CashVanProductRef(
    val id: String? = null,
    val name: String? = null,
    val sku: String? = null,
    val image_url: String? = null
)

@Serializable
data class CashVanProductPriceRow(
    val price_tier: String? = null,
    val governorate_id: String? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val final_price: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val base_price: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val cash_discount: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val vat_percentage: Double? = null,
    /** `exclusive` or `inclusive`. */
    val vat_application: String? = null,
    val discounted: Boolean? = null,
    val price_details: CashVanPriceDetails? = null
)

@Serializable
data class CashVanPriceDetails(
    val unit: CashVanUnitPrice? = null
)

@Serializable
data class CashVanUnitPrice(
    @Serializable(with = FlexibleDoubleSerializer::class) val discount_amount: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val vat_amount: Double? = null
)

/** `get_visit_recommendations`: the merchant's predicted spend and the products to suggest this visit. */
@Serializable
data class VisitRecommendationsRow(
    @Serializable(with = FlexibleDoubleSerializer::class) val last_visit_total_value: Double? = null,
    val last_visit_date: String? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val predicted_spending: Double? = null,
    val recommended_products: List<RecommendedProductRow>? = null,
    val off_truck_products: List<RecommendedProductRow>? = null
)

@Serializable
data class RecommendedProductRow(
    val product_id: String,
    val sku: String? = null,
    val name: String? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val score: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val default_qty: Double? = null
)

@Serializable
data class CashVanOrdersResponse(
    val data: List<CashVanOrderRow> = emptyList()
)

@Serializable
data class CashVanOrderRow(
    val id: String,
    val plan_id: String? = null,
    val order_type: String? = null,
    val status: String? = null,
    val formatted_code: String? = null,
    val created_at: String? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val total_income: Double? = null,
    val merchant: CashVanOrderMerchant? = null
)

@Serializable
data class CashVanOrderMerchant(
    val name: String? = null,
    val sign_name: String? = null
)
