package croniot.models

import croniot.models.dto.AccountDto
import kotlinx.serialization.Serializable

@Serializable
data class LoginResultDto(
    val result: Result,
    val accountDto: AccountDto?,
    val token: String?,
    val refreshToken: String? = null,
    val accessTokenExpiresAtEpochSeconds: Long? = null,
)

@Serializable
data class RefreshTokenRequestDto(
    val refreshToken: String,
)

@Serializable
data class RefreshTokenResultDto(
    val result: Result,
    val token: String? = null,
    val refreshToken: String? = null,
    val accessTokenExpiresAtEpochSeconds: Long? = null,
)

@Serializable
data class LogoutRequestDto(
    val refreshToken: String,
)
