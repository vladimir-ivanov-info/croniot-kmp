package croniot.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class FeatureFlagDto(
    val name: String,
    val enabled: Boolean,
    val description: String? = null,
)
