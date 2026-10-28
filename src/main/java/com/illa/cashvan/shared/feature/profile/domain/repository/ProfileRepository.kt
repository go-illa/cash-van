package com.illa.cashvan.shared.feature.profile.domain.repository

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.shared.feature.profile.data.model.ProfileResponse

interface ProfileRepository {
    suspend fun getProfile(salesAgentId: String): ApiResult<ProfileResponse>
}