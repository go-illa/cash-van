package com.illa.cashvan.feature.commission

import com.illa.cashvan.feature.commission.data.model.CommissionPayout
import com.illa.cashvan.feature.commission.domain.model.toFulfillmentCommission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FulfillmentCommissionTest {

    private fun payout(
        status: String? = "below_threshold",
        targetOrders: Int? = 75,
        actualOrders: Int? = 0,
        payoutEgp: Double? = 0.0,
        payoutAtThresholdEgp: Double? = 1000.0
    ) = CommissionPayout(
        def_name = "Fulfillment agents target 1",
        month = "2026-10-01",
        status = status,
        target_orders = targetOrders,
        actual_orders = actualOrders,
        payout_egp = payoutEgp,
        payout_at_threshold_egp = payoutAtThresholdEgp
    )

    @Test
    fun `progress is achieved orders out of target orders`() {
        val commission = payout(actualOrders = 55).toFulfillmentCommission()

        assertEquals(75, commission.targetOrders)
        assertEquals(55, commission.achievedOrders)
        assertEquals(73.33, commission.achievementPercentage, 0.01)
        assertEquals(20, commission.remainingOrders)
    }

    @Test
    fun `eligibility follows the control tower status, not the orders percentage`() {
        // 68 of 75 is above the 90% threshold configured in the Control Tower, yet the row is not met
        val commission = payout(actualOrders = 68).toFulfillmentCommission()

        assertFalse(commission.isEligible)
        assertEquals(0.0, commission.payoutEgp, 0.0)
        assertEquals(1000.0, commission.potentialPayoutEgp, 0.0)
    }

    @Test
    fun `met status makes the agent eligible for the payout`() {
        val commission = payout(status = "met", actualOrders = 92, payoutEgp = 1000.0).toFulfillmentCommission()

        assertTrue(commission.isEligible)
        assertEquals(1000.0, commission.payoutEgp, 0.0)
        assertEquals(122.67, commission.achievementPercentage, 0.01)
        assertEquals(0, commission.remainingOrders)
    }

    @Test
    fun `missing values default to zero`() {
        val commission = payout(
            status = null,
            targetOrders = null,
            actualOrders = null,
            payoutEgp = null,
            payoutAtThresholdEgp = null
        ).toFulfillmentCommission()

        assertFalse(commission.isEligible)
        assertEquals(0.0, commission.achievementPercentage, 0.0)
        assertEquals(0, commission.remainingOrders)
        assertEquals(0.0, commission.potentialPayoutEgp, 0.0)
    }
}
