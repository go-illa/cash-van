package com.illa.cashvan.feature.profile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileResponse(
    val sales_agent_name: String,
    val sales_agent_phone: String,
    val supervisor_name: String,
    val supervisor_phone: String
)