package com.illa.cashvan.shared.feature.profile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileResponse(
    val name: String,
    val phone_number: String,
    val supervisor: SupervisorResponse,
    // Not sent by the backend yet; used to refresh roles on app start once it is.
    val roles: List<String>? = null,
    val agent_id: String? = null
) {
    val sales_agent_name: String get() = name
    val sales_agent_phone: String get() = phone_number
    val supervisor_name: String get() = supervisor.name
    val supervisor_phone: String get() = supervisor.phone_number
}

@Serializable
data class SupervisorResponse(
    val name: String,
    val phone_number: String
)