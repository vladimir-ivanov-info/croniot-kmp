package com.server.croniot.services

import com.server.croniot.application.DomainException
import com.server.croniot.data.repositories.AccountRepository
import croniot.messages.MessageRegisterAccount
import croniot.models.Result
import croniot.models.errors.DomainError
import javax.inject.Inject

class AccountService @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    fun registerAccount(messageRegisterAccount: MessageRegisterAccount): Result {
        val email = messageRegisterAccount.email
        if (!accountRepository.isEmailAvailable(email)) {
            throw DomainException(DomainError.Conflict("This email is already used."))
        }
        accountRepository.createAccount(
            messageRegisterAccount.accountUuid,
            messageRegisterAccount.nickname,
            email,
            messageRegisterAccount.password,
        )
        return Result(true, "")
    }
}
