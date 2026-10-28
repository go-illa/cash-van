package com.illa.cashvan.sales.domain.cashvan

/** Cart items whose plan product is no longer offered, or no longer has a valid price. */
fun findInvalidCashvanCartItems(
    items: List<CashvanCartItem>,
    products: List<CashvanProduct>,
): List<CashvanCartItem> {
    val productsByPlanProductId = products.associateBy { it.planProductId }
    return items.filter { item -> productsByPlanProductId[item.planProductId]?.hasValidPrice != true }
}
