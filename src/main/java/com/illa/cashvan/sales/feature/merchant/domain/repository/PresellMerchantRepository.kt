package com.illa.cashvan.sales.feature.merchant.domain.repository

import com.illa.cashvan.sales.domain.merchant.GovernorateRegion
import com.illa.cashvan.sales.feature.merchant.data.model.AgentRouteOption
import com.illa.cashvan.sales.feature.merchant.data.model.NewMerchantForm
import com.illa.cashvan.sales.feature.merchant.data.model.PresellMerchantType

interface PresellMerchantRepository {
    suspend fun getMerchantTypes(): List<PresellMerchantType>

    /** Pricing regions with every name they go by, to match a governorate to a region. */
    suspend fun getRegions(): List<GovernorateRegion>

    suspend fun getAgentRoutes(): List<AgentRouteOption>

    /**
     * Creates the merchant, its visit days and its route link. A failed later step deletes what was
     * already created.
     * @return the new merchant id.
     */
    suspend fun createMerchant(form: NewMerchantForm, agentName: String?): String
}

class MerchantCreationException(val reason: Reason, cause: Throwable? = null) : Exception(reason.name, cause) {
    enum class Reason { NO_ROUTE, ROUTE_REQUIRED, VISIT_DAYS_FAILED, ROUTE_LINK_FAILED }
}
