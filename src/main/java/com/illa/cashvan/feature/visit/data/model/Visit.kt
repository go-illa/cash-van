package com.illa.cashvan.feature.visit.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NoOrderReason(
    val id: String,
    val reason_ar: String,
    val reason_en: String,
    val display_order: Int,
    val is_active: Boolean
)

@Serializable
data class NoOrderReasonsResponse(
    val no_order_reasons: List<NoOrderReason>
)

@Serializable
data class CreateVisitRequest(
    val visit: VisitData
)

@Serializable
data class VisitData(
    val merchant_id: String,
    val visit_type: String,
    val visit_date: String,
    val latitude: Double,
    val longitude: Double,
    val visit_quality: Int,
    val no_order_reason_id: String
)

@Serializable
data class CreateVisitResponse(
    val id: String,
    val visit_type: String? = null,
    val visit_date: String? = null,
    val merchant_id: String? = null
)
