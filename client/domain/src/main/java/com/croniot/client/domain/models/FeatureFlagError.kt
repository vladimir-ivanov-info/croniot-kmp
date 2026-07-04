package com.croniot.client.domain.models

sealed interface FeatureFlagError {
    data object Network : FeatureFlagError
    data object Unknown : FeatureFlagError
}