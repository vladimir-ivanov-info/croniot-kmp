package com.croniot.client.domain.usecases

import com.croniot.client.domain.repositories.FeatureFlagRepository

class FetchFeatureFlagsUseCase(
    private val featureFlagRepository: FeatureFlagRepository,
) {
    suspend operator fun invoke(): Result<Unit> = featureFlagRepository.fetchAndCache()
}
