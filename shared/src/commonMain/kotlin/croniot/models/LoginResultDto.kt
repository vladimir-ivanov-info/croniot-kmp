package croniot.models

import croniot.models.dto.AccountDto
import kotlinx.serialization.Serializable

@Serializable
data class LoginResultDto(
    val result: Result,
    val accountDto: AccountDto?,
    val token: String?,
)
