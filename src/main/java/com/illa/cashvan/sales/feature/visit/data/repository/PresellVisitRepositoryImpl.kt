package com.illa.cashvan.sales.feature.visit.data.repository

import com.illa.cashvan.core.location.LocationData
import com.illa.cashvan.core.supabase.SupabaseClient
import com.illa.cashvan.core.supabase.SupabaseException
import com.illa.cashvan.core.supabase.selectInChunks
import com.illa.cashvan.sales.data.CurrentAgentProvider
import com.illa.cashvan.sales.feature.visit.data.model.HistoryOrderItemRow
import com.illa.cashvan.sales.feature.visit.data.model.HistoryOrderRow
import com.illa.cashvan.sales.feature.visit.data.model.HistoryVisitRow
import com.illa.cashvan.sales.feature.visit.data.model.NewVisitRow
import com.illa.cashvan.sales.feature.visit.data.model.NoOrderReason
import com.illa.cashvan.sales.feature.visit.data.model.NoOrderUpdate
import com.illa.cashvan.sales.feature.visit.data.model.VisitHistoryItem
import com.illa.cashvan.sales.feature.visit.data.model.VisitHistoryOrder
import com.illa.cashvan.sales.feature.visit.data.model.VisitHistoryOrderItem
import com.illa.cashvan.sales.feature.visit.data.model.VisitIdRow
import com.illa.cashvan.sales.feature.visit.domain.repository.DuplicateVisitException
import com.illa.cashvan.sales.feature.visit.domain.repository.PresellVisitRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.Instant

class PresellVisitRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val currentAgentProvider: CurrentAgentProvider
) : PresellVisitRepository {

    override suspend fun createVisit(merchantId: String, location: LocationData, isAdHoc: Boolean): String {
        val agent = currentAgentProvider.require()
        val row = NewVisitRow(
            agent_id = agent.agentId,
            merchant_id = merchantId,
            visit_date = Instant.now().toString(),
            visit_type = WITH_ORDER,
            latitude = location.latitude,
            longitude = location.longitude,
            location = "${location.latitude},${location.longitude}",
            is_ad_hoc = isAdHoc
        )
        try {
            val inserted: List<VisitIdRow> = supabaseClient.insert("visits", row) { select("id") }
            return inserted.first().id
        } catch (e: SupabaseException) {
            if (e.isDuplicateVisit()) throw DuplicateVisitException(e)
            throw e
        }
    }

    override suspend fun completeVisitWithoutOrder(visitId: String, reasonId: String) {
        supabaseClient.update<NoOrderUpdate, VisitIdRow>("visits", NoOrderUpdate(WITHOUT_ORDER, reasonId)) {
            select("id")
            eq("id", visitId)
        }
    }

    override suspend fun getNoOrderReasons(): List<NoOrderReason> =
        supabaseClient.select("no_order_reasons") {
            select("id,reason_ar,reason_en,display_order")
            eq("is_active", true)
            order("display_order")
        }

    override suspend fun getVisitHistory(merchantId: String): List<VisitHistoryItem> = coroutineScope {
        val visits = supabaseClient.select<HistoryVisitRow>("visits") {
            select("id,visit_date,visit_type,no_order_reason_id")
            eq("merchant_id", merchantId)
            order("visit_date", ascending = false)
            limit(MAX_HISTORY_VISITS)
        }
        if (visits.isEmpty()) return@coroutineScope emptyList()

        val reasonIds = visits.mapNotNull { it.no_order_reason_id }.distinct()
        val reasons = async {
            if (reasonIds.isEmpty()) emptyList()
            else supabaseClient.select<NoOrderReason>("no_order_reasons") {
                select("id,reason_ar,reason_en")
                isIn("id", reasonIds)
            }
        }
        val orders = async {
            supabaseClient.select<HistoryOrderRow>("sales_orders") {
                select("id,visit_id,total_value,total_items,payment_type")
                isIn("visit_id", visits.map { it.id })
            }
        }
        val orderRows = orders.await()
        val items = supabaseClient.selectInChunks<HistoryOrderItemRow>("sales_order_items", orderRows.map { it.id }) { chunk ->
            select("sales_order_id,quantity,unit_price,product_id,products(name)")
            isIn("sales_order_id", chunk)
        }.groupBy { it.sales_order_id }

        val reasonById = reasons.await().associateBy { it.id }
        val ordersByVisit = orderRows.groupBy { it.visit_id }
        visits.map { visit ->
            VisitHistoryItem(
                visitId = visit.id,
                visitDate = visit.visit_date,
                withOrder = visit.visit_type == WITH_ORDER,
                noOrderReason = visit.no_order_reason_id?.let { reasonById[it]?.reason_ar },
                orders = ordersByVisit[visit.id].orEmpty().map { order ->
                    VisitHistoryOrder(
                        orderId = order.id,
                        totalValue = order.total_value ?: 0.0,
                        totalItems = order.total_items ?: 0,
                        paymentType = order.payment_type ?: "cash",
                        items = items[order.id].orEmpty().map {
                            VisitHistoryOrderItem(it.products?.name, it.quantity, it.unit_price)
                        }
                    )
                }
            )
        }
    }

    /** Supabase rejects a second visit to the same merchant within 5 minutes; these are the messages the PWA matches. */
    private fun SupabaseException.isDuplicateVisit(): Boolean {
        val text = message.lowercase()
        return "5 minutes" in text || "duplicate" in text || "already exists" in text
    }

    private companion object {
        const val WITH_ORDER = "with_order"
        const val WITHOUT_ORDER = "without_order"
        const val MAX_HISTORY_VISITS = 20
    }
}
