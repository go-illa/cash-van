package com.illa.cashvan.feature.kpi.data.repository

import com.illa.cashvan.feature.kpi.data.model.AgentKpiResponse

interface KpiRepository {
    suspend fun getAgentKpi(agentId: String, month: Int?, year: Int?): AgentKpiResponse
}
