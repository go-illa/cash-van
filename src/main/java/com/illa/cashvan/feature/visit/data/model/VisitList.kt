package com.illa.cashvan.feature.visit.data.model

import kotlinx.serialization.Serializable

@Serializable
data class VisitMerchant(
    val id: String? = null,
    val name: String? = null,
    val phone_number: String? = null
)

@Serializable
data class VisitNoOrderReason(
    val id: String? = null,
    val reason_ar: String? = null,
    val reason_en: String? = null
)

@Serializable
data class VisitSalesAgent(
    val id: String? = null,
    val name: String? = null
)

@Serializable
data class VisitItem(
    val id: String,
    val visit_type: String? = null,
    val visit_date: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val location: String? = null,
    val visit_quality: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val merchant: VisitMerchant? = null,
    val no_order_reason: VisitNoOrderReason? = null,
    val sales_agent: VisitSalesAgent? = null
)

@Serializable
data class VisitsMeta(
    val page: Int? = null,
    val limit: Int? = null,
    val last: Int? = null,
    val count: Int? = null
)

@Serializable
data class VisitsListResponse(
    val data: List<VisitItem>,
    val meta: VisitsMeta? = null
)
