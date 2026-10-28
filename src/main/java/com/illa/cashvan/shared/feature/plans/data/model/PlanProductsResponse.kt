package com.illa.cashvan.shared.feature.plans.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PlanProductsResponse(
    val plan_products: List<PlanProduct>
)