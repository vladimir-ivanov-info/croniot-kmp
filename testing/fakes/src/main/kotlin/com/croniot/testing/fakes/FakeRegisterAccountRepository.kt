package com.croniot.testing.fakes

import com.croniot.client.domain.repositories.RegisterAccountRepository
import croniot.models.Result

class FakeRegisterAccountRepository(
    private var result: Result = Result(success = true, message = ""),
) : RegisterAccountRepository {

    var registerAccountInvocations: MutableList<Triple<String, String, String>> = mutableListOf()
        private set

    override suspend fun registerAccount(nickname: String, email: String, password: String): Result {
        registerAccountInvocations += Triple(nickname, email, password)
        return result
    }
}
