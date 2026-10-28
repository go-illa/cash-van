package com.illa.cashvan.shared.feature.kpi.data.repository

import com.illa.cashvan.shared.feature.kpi.data.model.AgentKpiResponse

interface KpiRepository {
    suspend fun getAgentKpi(agentId: String, month: Int?, year: Int?): AgentKpiResponse
}
