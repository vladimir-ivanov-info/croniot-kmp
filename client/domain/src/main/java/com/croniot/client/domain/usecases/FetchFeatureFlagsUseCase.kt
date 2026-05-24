package com.croniot.client.domain.usecases

import Outcome
import com.croniot.client.domain.models.FeatureFlagError
import com.croniot.client.domain.repositories.FeatureFlagRepository

class FetchFeatureFlagsUseCase(
    private val featureFlagRepository: FeatureFlagRepository,
) {
    suspend operator fun invoke(): Outcome<Unit, FeatureFlagError> = featureFlagRepository.fetchAndCache()
}
