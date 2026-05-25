package com.illa.cashvan.feature.kpi.domain.usecase

import com.illa.cashvan.feature.kpi.data.model.AgentKpiResponse
import com.illa.cashvan.feature.kpi.data.repository.KpiRepository

class GetAgentKpiUseCase(private val repository: KpiRepository) {
    suspend operator fun invoke(agentId: String, month: Int? = null, year: Int? = null): AgentKpiResponse {
        return repository.getAgentKpi(agentId, month, year)
    }
}
