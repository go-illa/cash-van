package com.illa.cashvan.feature.plans.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Plan(
    val id: String,
    val created_at: String,
    val updated_at: String,
    val formatted_code: String,
    val infor_code: String,
    val status: String,
    val creator_id: String,
    val creator_type: String,
    val sales_agent_id: String
)