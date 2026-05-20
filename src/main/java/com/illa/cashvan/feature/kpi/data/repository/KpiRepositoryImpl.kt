package com.illa.cashvan.feature.kpi.data.repository

import com.illa.cashvan.feature.kpi.data.model.AgentKpiRequest
import com.illa.cashvan.feature.kpi.data.model.AgentKpiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class KpiRepositoryImpl(
    private val supabaseClient: HttpClient
) : KpiRepository {

    override suspend fun getAgentKpi(agentId: String, month: Int?, year: Int?): AgentKpiResponse {
        return supabaseClient.post("agent-kpi-mobile") {
            contentType(ContentType.Application.Json)
            setBody(AgentKpiRequest(agent_id = agentId, agent_type = "cashvan", month = month, year = year))
        }.body()
    }
}
