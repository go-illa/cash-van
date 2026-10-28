package com.illa.cashvan.sales.feature.orders.data.repository

import com.illa.cashvan.core.supabase.SupabaseClient
import com.illa.cashvan.sales.data.CurrentAgentProvider
import com.illa.cashvan.sales.feature.orders.data.model.CancelOrderResult
import com.illa.cashvan.sales.feature.orders.data.model.PresellHistoryOrder
import com.illa.cashvan.sales.feature.orders.domain.repository.CancelOrderException
import com.illa.cashvan.sales.feature.orders.domain.repository.PresellOrderRepository
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.ZoneId
import java.time.ZonedDateTime

class PresellOrderRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val currentAgentProvider: CurrentAgentProvider
) : PresellOrderRepository {

    override suspend fun getMonthOrders(): List<PresellHistoryOrder> {
        val agent = currentAgentProvider.require()
        val monthStart = ZonedDateTime.now(CAIRO).withDayOfMonth(1).toLocalDate().atStartOfDay(CAIRO)
        return supabaseClient.select("sales_orders") {
            select(PresellHistoryOrder.SELECT)
            eq("agent_id", agent.agentId)
            gte("order_date", monthStart.toInstant().toString())
            lt("order_date", monthStart.plusMonths(1).toInstant().toString())
            order("order_date", ascending = false)
        }
    }

    override suspend fun cancelOrder(orderId: String) {
        val agent = currentAgentProvider.require()
        val result = supabaseClient.rpc<CancelOrderResult>(
            "cancel_presell_order",
            buildJsonObject {
                put("p_order_id", orderId)
                put("p_agent_id", agent.agentId)
            }
        )
        if (!result.success) throw CancelOrderException(result.code ?: "CANCEL_FAILED")
    }

    private companion object {
        val CAIRO: ZoneId = ZoneId.of("Africa/Cairo")
    }
}
