package com.illa.cashvan.feature.plans.domain.repository

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.plans.data.model.PlansResponse

interface PlansRepository {
    suspend fun getPlans(): ApiResult<PlansResponse>
}