package com.illa.cashvan.sales.domain.cashvan

// Rails' PlanProduct#product / OrderPlanProduct#product associations are scoped
// by Product's Discard default_scope, so they come back null for a discarded
// product even though the ids are correct. These are the pure halves of the
// PWA's Supabase backfill: what to look up, and how to merge the result.

/** A `products` row selected as `id, name, sku, image_url`. */
data class SupabaseProductRow(
    val id: String,
    val name: String?,
    val sku: String?,
    val imageUrl: String?,
)

fun SupabaseProductRow.toRailsProduct(): RailsProduct =
    RailsProduct(id = id, name = name ?: "", sku = sku ?: "", imageUrl = imageUrl)

/** A `plan_products` row selected as `id, product_id`. */
data class PlanProductProductIdRow(val id: String, val productId: String?)

/** Distinct product ids (first-seen order) to fetch for plan products missing their product. */
fun missingPlanProductProductIds(items: List<RailsPlanProduct>): List<String> =
    items.filter { it.product == null }.map { it.productId }.distinct()

/** Fills missing products from [productById]; returns [items] itself when nothing was fetched. */
fun backfillPlanProducts(
    items: List<RailsPlanProduct>,
    productById: Map<String, RailsProduct>,
): List<RailsPlanProduct> {
    if (productById.isEmpty()) return items
    return items.map { item ->
        if (item.product != null) item else item.copy(product = productById[item.productId])
    }
}

/** Distinct plan-product ids (first-seen order) of order lines missing their product. */
fun missingOrderPlanProductIds(items: List<RailsOrderPlanProduct>): List<String> =
    items.filter { it.product == null }.map { it.planProductId }.distinct()

/** Distinct non-empty product ids of the fetched plan-product rows. */
fun planProductRowProductIds(rows: List<PlanProductProductIdRow>): List<String> =
    rows.mapNotNull { row -> row.productId?.takeIf { it.isNotEmpty() } }.distinct()

/**
 * Fills missing order-line products by resolving plan product → product;
 * returns [items] itself when nothing was fetched. The last row wins for a
 * duplicated plan-product id, like the TS `new Map(rows)`.
 */
fun backfillOrderPlanProducts(
    items: List<RailsOrderPlanProduct>,
    planProductRows: List<PlanProductProductIdRow>,
    productById: Map<String, RailsProduct>,
): List<RailsOrderPlanProduct> {
    if (productById.isEmpty()) return items
    val productIdByPlanProductId = planProductRows.associate { it.id to it.productId }
    return items.map { item ->
        if (item.product != null) return@map item
        val product = productIdByPlanProductId[item.planProductId]
            ?.takeIf { it.isNotEmpty() }
            ?.let { productById[it] }
        if (product != null) item.copy(product = product) else item
    }
}
