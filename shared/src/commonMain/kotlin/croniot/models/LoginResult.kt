package croniot.models

import croniot.models.dto.AccountDto

data class LoginResult(val result: Result, val account: AccountDto?, val token: String?)
