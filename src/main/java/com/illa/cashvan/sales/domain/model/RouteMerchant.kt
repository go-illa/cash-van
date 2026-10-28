package com.illa.cashvan.sales.domain.model

import com.illa.cashvan.sales.domain.route.MerchantVisitProgress

/**
 * A merchant on the agent's monthly journey plan, with this month's regular (non ad-hoc) visits.
 */
data class RouteMerchant(
    val merchantId: String,
    val merchantCode: String,
    val merchantName: String,
    val signName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val location: String?,
    val priceTier: String?,
    val phoneNumber: String?,
    val secondaryPhone: String?,
    val detailedAddress: String?,
    val governorate: String?,
    val frontdoorCode: String?,
    val googleLink: String?,
    val workingDays: List<String>,
    val regionId: String?,
    val fawryDeviceNumber: String?,
    val wantsFawryDevice: Boolean?,
    val routeId: String,
    val routeName: String,
    /** Capped at [VISITS_PER_MERCHANT_PER_MONTH]. */
    override val visitCount: Int,
    /** ISO timestamp of the latest regular visit this month. */
    val lastVisitDate: String?,
    val hasOrder: Boolean,
    override val targetCompleted: Boolean
) : MerchantVisitProgress {
    val displayName: String get() = signName?.takeIf { it.isNotBlank() } ?: merchantName

    val hasCoordinates: Boolean get() = latitude != null && longitude != null
}

/** Planned visits per merchant per month. */
const val VISITS_PER_MERCHANT_PER_MONTH = 2

enum class VisitProgressStatus { UNVISITED, IN_PROGRESS, COMPLETED }

data class VisitProgress(
    val count: Int,
    val target: Int,
    val status: VisitProgressStatus
)

fun visitProgress(
    visitCount: Int?,
    target: Int = VISITS_PER_MERCHANT_PER_MONTH
): VisitProgress {
    val safeTarget = maxOf(1, target)
    val count = (visitCount ?: 0).coerceIn(0, safeTarget)
    val status = when {
        count == 0 -> VisitProgressStatus.UNVISITED
        count >= safeTarget -> VisitProgressStatus.COMPLETED
        else -> VisitProgressStatus.IN_PROGRESS
    }
    return VisitProgress(count, safeTarget, status)
}
