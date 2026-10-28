package com.illa.cashvan.sales.domain.route

enum class MerchantFilterStatus(val wireValue: String) {
    ALL("all"),
    UNVISITED("unvisited"),
    VISITED("visited"),
    COMPLETED("completed"),
}

/** Structural stand-in for the TS `{ visit_count, target_completed }` merchant shape. */
interface MerchantVisitProgress {
    val visitCount: Int
    val targetCompleted: Boolean
}

/** Resolves a deep-link/query value; anything unsupported (including null) is [MerchantFilterStatus.ALL]. */
fun resolveMerchantFilterStatus(value: String?): MerchantFilterStatus =
    MerchantFilterStatus.entries.firstOrNull { it.wireValue == value } ?: MerchantFilterStatus.ALL

fun matchesMerchantFilter(merchant: MerchantVisitProgress, status: MerchantFilterStatus): Boolean = when (status) {
    MerchantFilterStatus.UNVISITED -> merchant.visitCount == 0
    MerchantFilterStatus.VISITED -> merchant.visitCount > 0 && !merchant.targetCompleted
    MerchantFilterStatus.COMPLETED -> merchant.targetCompleted
    MerchantFilterStatus.ALL -> true
}
