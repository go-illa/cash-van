package com.illa.cashvan.sales.domain.cart

// DIS-446: pure cart repricing mapper. Given fresh product_prices rows for a
// tier/region, produces the next cart state plus the items that have no price
// under those coordinates. Replace-everything semantics: every item with a
// fresh row is repriced, not just ones missing price info.

/** Full product_prices row — same shape stored at add-to-cart time. */
typealias ProductPriceRow = PriceInfo

data class CartProduct(
    val id: String,
    val description: String,
    val sku: String,
    val price: Double,
    val principalId: String?,
    val imageUrl: String?,
    val priceInfo: PriceInfo? = null,
)

data class CartItem(
    val productId: String,
    val quantity: Int,
    val product: CartProduct,
    val principalName: String,
)

data class FreshPricesResult(
    /** Next cart items — every item with a fresh row is repriced; others pass through unchanged. */
    val updatedItems: List<CartItem>,
    /** Descriptions of items that have no price row under the requested tier/region. */
    val unfixableDescriptions: List<String>,
)

fun applyFreshPrices(items: List<CartItem>, priceRows: List<ProductPriceRow>): FreshPricesResult {
    // associateBy keeps the last row per product, like the TS Map.set loop.
    val priceMap = priceRows.associateBy { it.productId }
    val unfixableDescriptions = mutableListOf<String>()

    val updatedItems = items.map { item ->
        val fresh = priceMap[item.productId]
        if (fresh == null) {
            unfixableDescriptions += item.product.description
            item
        } else {
            item.copy(product = item.product.copy(price = getFinalPrice(fresh), priceInfo = fresh))
        }
    }

    return FreshPricesResult(updatedItems, unfixableDescriptions)
}
