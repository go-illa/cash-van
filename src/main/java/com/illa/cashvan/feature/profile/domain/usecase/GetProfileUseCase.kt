package com.illa.cashvan.feature.profile.domain.usecase

import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.profile.data.model.ProfileResponse
import com.illa.cashvan.feature.profile.domain.repository.ProfileRepository

class GetProfileUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(salesAgentId: String): ApiResult<ProfileResponse> {
        return profileRepository.getProfile(salesAgentId)
    }
}