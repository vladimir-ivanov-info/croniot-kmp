package croniot.models

import croniot.models.dto.AccountDto

data class LoginResultDto(
    val result: Result,
    val accountDto: AccountDto?,
    val token: String?,
)