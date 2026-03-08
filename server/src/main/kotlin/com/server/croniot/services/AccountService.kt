package com.server.croniot.services

import com.server.croniot.data.repositories.AccountRepository
import croniot.messages.MessageRegisterAccount
import croniot.models.Result
import javax.inject.Inject

class AccountService @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    fun registerAccount(messageRegisterAccount: MessageRegisterAccount): Result {
        try {
            val accountUuid = messageRegisterAccount.accountUuid
            val nickname = messageRegisterAccount.nickname
            val email = messageRegisterAccount.email
            val password = messageRegisterAccount.password

            if (!accountRepository.isEmailAvailable(email)) {
                return Result(false, "This email is already used.")
            }

            accountRepository.createAccount(accountUuid, nickname, email, password)
            return Result(true, "")
        } catch (e: Throwable) {
            return Result(false, "Could not register account")
        }
    }
}
