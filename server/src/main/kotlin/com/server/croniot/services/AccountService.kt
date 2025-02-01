package com.server.croniot.services

import com.google.gson.GsonBuilder
import com.server.croniot.data.repositories.AccountRepository
import com.server.croniot.data.repositories.DeviceTokenRepository
import croniot.messages.MessageGetAccountInfo
import croniot.messages.MessageRegisterAccount
import croniot.models.Result
import javax.inject.Inject

class AccountService @Inject constructor(
    private val accountRepository: AccountRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
) {
    // TODO CRUD

    fun registerAccount(messageRegisterAccount: MessageRegisterAccount): Result {
        var result: Result

        try {
            val accountUuid = messageRegisterAccount.accountUuid
            val nickname = messageRegisterAccount.nickname
            val email = messageRegisterAccount.email
            val password = messageRegisterAccount.password

            // TODO check email not empty, etc.

            if (!accountRepository.isEmailAvailable(email)) {
                result = Result(false, "This email is already used.")
            } else {
                accountRepository.createAccount(accountUuid, nickname, email, password)
                result = Result(true, "")
            }
        } catch (e: Throwable) {
            result = Result(false, "Could not register account")
        }

        return result
    }

    fun processAccountInfoRequest(message: MessageGetAccountInfo): Result {
        val token = message.token
        val device = deviceTokenRepository.getDeviceAssociatedWithToken(token)

        var result = Result(false, "")

        device?.let {
            val accounts = accountRepository.getAccountOfDevice(it) // TODOOO

            if (accounts.isNotEmpty()) {
                val account = accounts.first()
                val accountJson = GsonBuilder().setPrettyPrinting().create().toJson(account)
                result = Result(true, accountJson)
            } else {
                result = Result(true, "Could not get account.")
            }
        }
        return result
    }
}
