package com.illa.cashvan.sales.domain.cashvan

import com.illa.cashvan.sales.domain.format.jsTrim
import kotlin.math.floor

/** One entry of the leading "موصى به لهذه الزيارة" section. */
data class RecommendedSectionCard(
    val product: CashvanProduct,
    /** True for `recommended_products` entries; false for informational sold-out off-truck cards. */
    val isRecommended: Boolean,
    /** Floor of the RPC's `default_qty`; null when absent or not > 0. */
    val recommendedQuantity: Int?,
)

/** Floor of a usable `default_qty`, or null when it is absent, non-finite or not > 0. */
internal fun suggestedQuantity(defaultQty: Double?): Int? =
    defaultQty?.takeIf { it.isFinite() && it > 0 }?.let { floor(it).toInt() }

/**
 * DIS-405: available recommendations and off-truck products merged and
 * ordered by score DESC (product id ASC on ties), each joined to its truck
 * plan product. Entries absent from the plan or without a valid price are
 * dropped. Empty while searching, so every product stays findable in the grid.
 */
fun buildRecommendedSectionCards(
    products: List<CashvanProduct>?,
    recommendations: VisitRecommendationsResult?,
    searchQuery: String,
): List<RecommendedSectionCard> {
    if (products.isNullOrEmpty()) return emptyList()
    if (searchQuery.jsTrim().isNotEmpty()) return emptyList()

    val byProductId = products.associateBy { it.productId }
    val entries = recommendations?.recommendedProducts.orEmpty().map { it to false } +
        recommendations?.offTruckProducts.orEmpty().map { it to true }

    return entries
        .sortedWith { (a, _), (b, _) ->
            val scoreDiff = b.score - a.score
            when {
                scoreDiff > 0 -> 1
                scoreDiff < 0 -> -1
                else -> a.productId.compareTo(b.productId)
            }
        }
        .mapNotNull { (rec, offTruck) ->
            val product = byProductId[rec.productId]?.takeIf { it.hasValidPrice } ?: return@mapNotNull null
            RecommendedSectionCard(
                product = product,
                isRecommended = !offTruck,
                recommendedQuantity = suggestedQuantity(rec.defaultQty),
            )
        }
}

/**
 * DIS-405: drops section products from the main grid so nothing appears twice.
 * Returns [gridProducts] itself when the section is empty.
 */
fun <T> excludeSectionProducts(
    gridProducts: List<T>,
    sectionCards: List<RecommendedSectionCard>,
    productId: (T) -> String,
): List<T> {
    val sectionIds = sectionCards.mapTo(HashSet()) { it.product.productId }
    if (sectionIds.isEmpty()) return gridProducts
    return gridProducts.filterNot { productId(it) in sectionIds }
}
