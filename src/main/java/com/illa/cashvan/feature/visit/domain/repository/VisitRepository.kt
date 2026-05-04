package com.illa.cashvan.feature.visit.domain.repository

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.visit.data.model.CreateVisitRequest
import com.illa.cashvan.feature.visit.data.model.CreateVisitResponse
import com.illa.cashvan.feature.visit.data.model.NoOrderReasonsResponse
import com.illa.cashvan.feature.visit.data.model.VisitItem
import com.illa.cashvan.feature.visit.data.model.VisitsListResponse

interface VisitRepository {
    suspend fun getNoOrderReasons(): ApiResult<NoOrderReasonsResponse>
    suspend fun createVisit(request: CreateVisitRequest): ApiResult<CreateVisitResponse>
    suspend fun getVisits(page: Int, items: Int): ApiResult<VisitsListResponse>
    suspend fun getVisitById(id: String): ApiResult<VisitItem>
}
