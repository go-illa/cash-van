package com.illa.cashvan.feature.plans.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Plan(
    val id: Int,
    val created_at: String,
    val updated_at: String,
    val formatted_code: String,
    val infor_code: String,
    val status: String,
    val creator_id: Int,
    val creator_type: String,
    val sales_agent_id: Int
)