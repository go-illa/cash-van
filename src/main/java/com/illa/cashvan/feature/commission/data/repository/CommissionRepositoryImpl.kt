package com.illa.cashvan.feature.commission.data.repository

import com.illa.cashvan.core.network.di.SUPABASE_URL
import com.illa.cashvan.feature.commission.data.model.CommissionPayout
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class CommissionRepositoryImpl(
    private val supabaseClient: HttpClient
) : CommissionRepository {

    // The Control Tower leaves agent_id empty on fulfillment rows, so the agent is matched by name
    override suspend fun getFulfillmentCommission(agentName: String, month: String): List<CommissionPayout> {
        return supabaseClient.get("$SUPABASE_URL/rest/v1/commission_payouts") {
            expectSuccess = true
            parameter(
                "select",
                "def_name,month,status,target_orders,actual_orders,payout_egp,payout_at_threshold_egp"
            )
            parameter("role", "eq.fulfillment")
            parameter("is_live", "eq.true")
            parameter("month", "eq.$month")
            parameter("agent_name", "eq.$agentName")
            parameter("order", "def_name.asc")
        }.body()
    }
}
