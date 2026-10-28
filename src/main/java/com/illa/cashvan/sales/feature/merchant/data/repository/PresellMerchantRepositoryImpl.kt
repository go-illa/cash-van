package com.illa.cashvan.sales.feature.merchant.data.repository

import com.illa.cashvan.core.supabase.SupabaseClient
import com.illa.cashvan.sales.domain.merchant.GovernorateRegion
import com.illa.cashvan.sales.data.CurrentAgentProvider
import com.illa.cashvan.sales.feature.merchant.data.model.AgentRouteOption
import com.illa.cashvan.sales.feature.merchant.data.model.MerchantIdRow
import com.illa.cashvan.sales.feature.merchant.data.model.MerchantRouteLink
import com.illa.cashvan.sales.feature.merchant.data.model.MerchantVisitDayRow
import com.illa.cashvan.sales.feature.merchant.data.model.NewMerchantForm
import com.illa.cashvan.sales.feature.merchant.data.model.NewMerchantRow
import com.illa.cashvan.sales.feature.merchant.data.model.PresellMerchantType
import com.illa.cashvan.sales.feature.merchant.data.model.RegionRow
import com.illa.cashvan.sales.feature.merchant.domain.repository.MerchantCreationException
import com.illa.cashvan.sales.feature.merchant.domain.repository.MerchantCreationException.Reason
import com.illa.cashvan.sales.feature.merchant.domain.repository.PresellMerchantRepository
import java.time.Instant

class PresellMerchantRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val currentAgentProvider: CurrentAgentProvider
) : PresellMerchantRepository {

    override suspend fun getMerchantTypes(): List<PresellMerchantType> =
        supabaseClient.select("merchant_types") {
            select("id,name")
            order("name")
        }

    override suspend fun getRegions(): List<GovernorateRegion> =
        supabaseClient.select<RegionRow>("regions") { select("id,name,arabic_name,english_name") }
            .map { GovernorateRegion(it.id, listOfNotNull(it.arabic_name, it.english_name, it.name)) }

    override suspend fun getAgentRoutes(): List<AgentRouteOption> {
        val agent = currentAgentProvider.require()
        return supabaseClient.select("agent_routes") {
            select("route_id,routes(name)")
            eq("agent_id", agent.agentId)
        }
    }

    override suspend fun createMerchant(form: NewMerchantForm, agentName: String?): String {
        val agent = currentAgentProvider.require()
        // Read routes fresh: the merchant must land on one of the agent's current routes.
        val routeIds = getAgentRoutes().map { it.route_id }
        if (routeIds.isEmpty()) throw MerchantCreationException(Reason.NO_ROUTE)
        val routeId = form.routeId ?: routeIds.singleOrNull() ?: throw MerchantCreationException(Reason.ROUTE_REQUIRED)

        val isRetail = form.priceTier == RETAIL
        val row = NewMerchantRow(
            name = form.merchantName,
            sign_name = form.signName,
            merchant_type_id = form.merchantTypeId,
            phone_number = form.phoneNumber,
            secondary_phone = form.secondaryPhone?.takeIf { it.isNotBlank() },
            governorate = form.governorate,
            region_id = form.regionId.ifBlank { null },
            route_id = routeId,
            detailed_address = form.detailedAddress,
            location = form.detailedAddress,
            latitude = form.latitude,
            longitude = form.longitude,
            created_by_agent_id = agent.agentId,
            created_by_agent_name = agentName,
            created_by_agent_code = agent.agentCode,
            price_tier = RETAIL,
            requested_price_tier = if (isRetail) null else form.priceTier,
            tier_request_status = if (isRetail) null else "pending",
            tier_requested_at = if (isRetail) null else Instant.now().toString()
        )
        val merchantId = supabaseClient.insert<NewMerchantRow, MerchantIdRow>("merchants", row) { select("id") }
            .first().id

        if (form.visitDays.isNotEmpty()) {
            try {
                supabaseClient.insert<List<MerchantVisitDayRow>, MerchantVisitDayRow>(
                    "merchant_visit_days",
                    form.visitDays.map { MerchantVisitDayRow(merchantId, it) }
                ) { select("merchant_id,day_of_week") }
            } catch (e: Exception) {
                deleteWhere("merchants", "id", merchantId)
                throw MerchantCreationException(Reason.VISIT_DAYS_FAILED, e)
            }
        }

        try {
            supabaseClient.insert<MerchantRouteLink, MerchantRouteLink>(
                "merchant_routes",
                MerchantRouteLink(merchantId, routeId)
            ) { select("merchant_id,route_id") }
        } catch (e: Exception) {
            deleteWhere("merchant_visit_days", "merchant_id", merchantId)
            deleteWhere("merchants", "id", merchantId)
            throw MerchantCreationException(Reason.ROUTE_LINK_FAILED, e)
        }

        return merchantId
    }

    private suspend fun deleteWhere(table: String, column: String, value: String) {
        runCatching { supabaseClient.delete(table) { eq(column, value) } }
    }

    private companion object {
        const val RETAIL = "retail"
    }
}
