package com.illa.cashvan.shared.feature.plans.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PlansResponse(
    val plans: List<Plan>
)