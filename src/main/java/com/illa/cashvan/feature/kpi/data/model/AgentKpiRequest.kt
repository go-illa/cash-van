package com.illa.cashvan.feature.kpi.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AgentKpiRequest(
    val agent_id: String,
    val agent_type: String? = null,
    val month: Int? = null,
    val year: Int? = null
)
