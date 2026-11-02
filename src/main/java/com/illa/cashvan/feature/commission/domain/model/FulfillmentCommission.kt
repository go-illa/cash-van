package com.illa.cashvan.feature.commission.domain.model

import com.illa.cashvan.feature.commission.data.model.CommissionPayout

data class FulfillmentCommission(
    val targetName: String,
    val targetOrders: Int,
    val achievedOrders: Int,
    val achievementPercentage: Double,
    val remainingOrders: Int,
    val isEligible: Boolean,
    val payoutEgp: Double,
    val potentialPayoutEgp: Double
)

private const val STATUS_MET = "met"

// Eligibility comes from the Control Tower status rather than being recomputed here: its unlock
// rule combines the orders target with other conditions that are not exposed to the app
fun CommissionPayout.toFulfillmentCommission(): FulfillmentCommission {
    val target = target_orders ?: 0
    val achieved = actual_orders ?: 0
    return FulfillmentCommission(
        targetName = def_name.orEmpty(),
        targetOrders = target,
        achievedOrders = achieved,
        achievementPercentage = if (target > 0) achieved * 100.0 / target else 0.0,
        remainingOrders = (target - achieved).coerceAtLeast(0),
        isEligible = status == STATUS_MET,
        payoutEgp = payout_egp ?: 0.0,
        potentialPayoutEgp = payout_at_threshold_egp ?: 0.0
    )
}
