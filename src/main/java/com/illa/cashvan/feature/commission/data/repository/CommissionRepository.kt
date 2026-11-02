package com.illa.cashvan.feature.commission.data.repository

import com.illa.cashvan.feature.commission.data.model.CommissionPayout

interface CommissionRepository {
    suspend fun getFulfillmentCommission(agentName: String, month: String): List<CommissionPayout>
}
