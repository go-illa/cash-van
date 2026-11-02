package com.illa.cashvan.feature.commission.data.model

import kotlinx.serialization.Serializable

/**
 * A live row of the Supabase `commission_payouts` table, written by the Control Tower
 * commission sync. For fulfillment agents the target is measured in orders.
 */
@Serializable
data class CommissionPayout(
    val def_name: String? = null,
    val month: String,
    val status: String? = null,
    val target_orders: Int? = null,
    val actual_orders: Int? = null,
    val payout_egp: Double? = null,
    val payout_at_threshold_egp: Double? = null
)
