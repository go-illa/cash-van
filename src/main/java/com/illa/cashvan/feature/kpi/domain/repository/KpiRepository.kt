package com.illa.cashvan.feature.kpi.domain.repository

import com.illa.cashvan.feature.kpi.data.model.AgentKpiResponse

interface KpiRepository {
    suspend fun getAgentKpi(agentId: String, month: Int?, year: Int?): AgentKpiResponse
}
