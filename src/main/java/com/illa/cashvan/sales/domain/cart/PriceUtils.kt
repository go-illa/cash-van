package com.illa.cashvan.sales.domain.cart

enum class PriceTier(val wireValue: String) {
    RETAIL("retail"),
    WHOLESALE("wholesale"),
    BIG_WHOLESALE("big_wholesale"),
    LARGE_GROCERY("large_grocery"),
    ;

    companion object {
        fun fromWire(value: String?): PriceTier? = entries.firstOrNull { it.wireValue == value }
    }
}

/**
 * A `product_prices` row. Covers both the PWA's `PriceInfo` and the superset
 * `ProductPriceRow` (which adds [governorateId]).
 */
data class PriceInfo(
    val id: String,
    val productId: String,
    val priceTier: PriceTier,
    val price: Double,
    val basePrice: Double?,
    val finalPrice: Double?,
    val discounted: Boolean,
    /** Raw JSON of `discount_details`; opaque to domain logic. */
    val discountDetails: String?,
    val vatPercentage: Double,
    val vatApplication: Int,
    val startDate: String?,
    val endDate: String?,
    val active: Boolean,
    val discountType: Int?,
    val cashDiscount: Double?,
    val governorateId: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * The final selling price for a product. Only `final_price` is used — rows with
 * a null `final_price` are filtered out at query level, so null resolves to 0.
 */
fun getFinalPrice(priceInfo: PriceInfo?): Double = priceInfo?.finalPrice ?: 0.0
