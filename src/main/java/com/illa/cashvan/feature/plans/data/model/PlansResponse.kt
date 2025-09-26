package com.illa.cashvan.feature.plans.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PlansResponse(
    val plans: List<Plan>
)