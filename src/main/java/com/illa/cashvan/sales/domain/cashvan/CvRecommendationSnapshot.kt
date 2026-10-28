package com.illa.cashvan.sales.domain.cashvan

// DIS-405: order-level recommendation snapshot, embedded as ONE structured
// property on the recommendation-tracking events: what the engine offered (and
// suggested per product), what the merchant took, and the merchant's total.

data class CvRecommendedProductSnapshot(
    val productId: String,
    val sku: String,
    val name: String,
    /** Units the engine suggested (floored `default_qty`; 0 when absent). */
    val recommendedQuantity: Int,
    /** Units the merchant took (0 when not taken). */
    val selectedQuantity: Int,
)

data class CvSelectedProductSnapshot(
    val productId: String,
    val sku: String,
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val isRecommended: Boolean,
)

data class CvRecommendationSnapshot(
    /** Offered-vs-taken per recommended product; null when there is no payload ([] = engine offered nothing). */
    val recommendedProducts: List<CvRecommendedProductSnapshot>?,
    /** The engine's predicted spending for this visit, if any. */
    val recommendedTotalSpending: Double?,
    /** Every cart line at send time. */
    val selectedProducts: List<CvSelectedProductSnapshot>,
    /** Cart/order total at send time: post-rebate on order events, pre-rebate on checkout-flush events. */
    val merchantTotalAmount: Double,
)

/** Builds the snapshot from the live cart and the visit recommendations payload. */
fun buildCvRecommendationSnapshot(
    items: List<CashvanCartItem>,
    recommendations: VisitRecommendationsResult?,
    merchantTotalAmount: Double,
): CvRecommendationSnapshot {
    val recommendedIds = recommendations?.recommendedProducts.orEmpty().mapTo(HashSet()) { it.productId }

    val recommendedProducts = recommendations?.recommendedProducts?.map { rec ->
        CvRecommendedProductSnapshot(
            productId = rec.productId,
            sku = rec.sku,
            name = rec.name,
            recommendedQuantity = suggestedQuantity(rec.defaultQty) ?: 0,
            selectedQuantity = items.firstOrNull { it.productId == rec.productId }?.quantity ?: 0,
        )
    }

    return CvRecommendationSnapshot(
        recommendedProducts = recommendedProducts,
        recommendedTotalSpending = recommendations?.predictedSpending,
        selectedProducts = items.map { item ->
            CvSelectedProductSnapshot(
                productId = item.productId,
                sku = item.sku,
                name = item.productName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                isRecommended = item.productId in recommendedIds,
            )
        },
        merchantTotalAmount = merchantTotalAmount,
    )
}

/** The snapshot keyed exactly as the PWA sends it (`recommendation_snapshot` event property). */
fun CvRecommendationSnapshot.toEventProperty(): Map<String, Any?> = mapOf(
    "recommended_products" to recommendedProducts?.map {
        mapOf(
            "product_id" to it.productId,
            "sku" to it.sku,
            "name" to it.name,
            "recommended_quantity" to it.recommendedQuantity,
            "selected_quantity" to it.selectedQuantity,
        )
    },
    "recommended_total_spending" to recommendedTotalSpending,
    "selected_products" to selectedProducts.map {
        mapOf(
            "product_id" to it.productId,
            "sku" to it.sku,
            "name" to it.name,
            "quantity" to it.quantity,
            "unit_price" to it.unitPrice,
            "is_recommended" to it.isRecommended,
        )
    },
    "merchant_total_amount" to merchantTotalAmount,
)
