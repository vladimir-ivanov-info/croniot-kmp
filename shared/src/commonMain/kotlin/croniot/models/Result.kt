package croniot.models

import kotlinx.serialization.Serializable

@Serializable
data class Result(val success: Boolean, val message: String = "")
