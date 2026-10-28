package com.illa.cashvan.sales.domain.cashvan

// Minimal Kotlin shapes of the PWA's src/types/cashvan.ts and the
// get_visit_recommendations payload: only the fields the ported logic reads.
// Rails decimals often arrive as strings ("125.50"); convert them with
// finiteNumberOrNull(String?) so the ported number semantics are preserved.

data class RailsProduct(
    val id: String,
    val name: String,
    val sku: String,
    val imageUrl: String?,
)

enum class CashvanPlanType(val wireValue: String) {
    CASH_VAN("cash_van"),
    PRE_SELL("pre_sell"),
    HYBRID("hybrid"),
    ;

    companion object {
        fun fromWire(value: String?): CashvanPlanType? = entries.firstOrNull { it.wireValue == value }
    }
}

data class RailsPlanProduct(
    val id: String,
    val productId: String,
    val assignedQuantity: Double? = null,
    val soldQuantity: Double? = null,
    val totalIncome: Double? = null,
    val totalAvailableQuantity: Double? = null,
    val preSellAvailableQuantity: Double? = null,
    val cashVanAvailableQuantity: Double? = null,
    /** Optional channel supplement: production responses may omit it, even for hybrid plans. */
    val preSellSoldQuantity: Double? = null,
    /** Optional channel supplement: production responses may omit it, even for hybrid plans. */
    val cashVanSoldQuantity: Double? = null,
    /** Null when Rails scoped out a discarded product (see [backfillPlanProducts]). */
    val product: RailsProduct? = null,
)

/**
 * `payment_type` exactly as the API sent it: the integer enum code on
 * order-detail/history endpoints, or the enum key on create. The two forms are
 * not interchangeable: the string "1" is not the code 1.
 */
sealed interface RawPaymentType {
    data class Code(val code: Int) : RawPaymentType

    data class Key(val key: String) : RawPaymentType
}

data class RailsMerchant(
    val id: String,
    val name: String,
    val signName: String? = null,
    val phoneNumber: String? = null,
    /** DIS-520: not serialized by Rails; backfilled from Supabase. */
    val fawryDeviceNumber: String? = null,
    /** DIS-520: not serialized by Rails; backfilled from Supabase. */
    val wantsFawryDevice: Boolean? = null,
)

data class RailsOrderPlanProductPriceTotals(
    val discountAmount: Double,
    val cashDiscountAmount: Double,
    val vatAmount: Double,
)

data class RailsOrderPlanProductPriceDetails(val total: RailsOrderPlanProductPriceTotals)

data class RailsOrderPlanProduct(
    val id: String,
    val planProductId: String,
    val quantity: Int? = null,
    val quantitySold: Int? = null,
    val soldQuantity: Int? = null,
    val totalIncome: Double? = null,
    val totalPriceDetails: RailsOrderPlanProductPriceDetails? = null,
    /** Null when Rails scoped out a discarded product (see [backfillOrderPlanProducts]). */
    val product: RailsProduct? = null,
)

/** The originating pre-sell order; its payment type, not the fulfillment order's, is authoritative. */
data class RailsPreSellSalesOrder(
    val id: String,
    val status: String,
    val paymentType: String,
)

data class RailsOrder(
    val id: String,
    val formattedCode: String,
    val orderType: String?,
    val status: String,
    val paymentType: RawPaymentType?,
    val totalIncome: Double?,
    val createdAt: String,
    val rebateValue: Double? = null,
    val merchant: RailsMerchant? = null,
    val orderPlanProducts: List<RailsOrderPlanProduct>? = null,
    /** Present only for pre_sell orders. */
    val salesOrder: RailsPreSellSalesOrder? = null,
)

data class CashvanPriceDetails(
    val basePrice: Double,
    val finalPrice: Double,
    val vatPercentage: Double,
    val vatAmount: Double,
    val vatIsExclusive: Boolean,
    val discountAmount: Double,
    val cashDiscount: Double,
)

data class CashvanProduct(
    val planProductId: String,
    val productId: String,
    val productName: String,
    val sku: String,
    val imageUrl: String?,
    val assignedQuantity: Int,
    val soldQuantity: Int,
    val cashVanAvailable: Int,
    val price: Double,
    val hasValidPrice: Boolean,
    val priceDetails: CashvanPriceDetails,
)

data class CashvanCartItem(
    val planProductId: String,
    val productId: String,
    val productName: String,
    val sku: String,
    val imageUrl: String?,
    val quantity: Int,
    val maxQuantity: Int,
    val unitPrice: Double,
    val priceDetails: CashvanPriceDetails,
)

/** One product of the `get_visit_recommendations` RPC payload. */
data class RecommendedProduct(
    val productId: String,
    val sku: String,
    val name: String,
    val score: Double,
    val defaultQty: Double? = null,
)

data class VisitRecommendationsResult(
    val predictedSpending: Double? = null,
    val recommendedProducts: List<RecommendedProduct>? = null,
    /** DIS-405: high-score products not sellable from this truck (absent from the plan, or sold out). */
    val offTruckProducts: List<RecommendedProduct>? = null,
)
