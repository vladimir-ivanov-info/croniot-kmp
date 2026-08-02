package com.croniot.client.domain.usecases

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.croniot.testing.fakes.FakeRegisterAccountRepository
import croniot.models.Result
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class RegisterAccountUseCaseTest {

    @Test
    fun `WHEN repository returns success THEN it returns that success result`() = runTest {
        val repository = FakeRegisterAccountRepository(result = Result(success = true, message = ""))
        val useCase = RegisterAccountUseCase(repository)

        val result = useCase("nickname", "user@example.com", "password123")

        assertThat(result.success).isTrue()
    }

    @Test
    fun `WHEN repository returns failure THEN it returns that failure result`() = runTest {
        val repository = FakeRegisterAccountRepository(result = Result(success = false, message = "Email taken"))
        val useCase = RegisterAccountUseCase(repository)

        val result = useCase("nickname", "user@example.com", "password123")

        assertThat(result.success).isFalse()
        assertThat(result.message).isEqualTo("Email taken")
    }

    @Test
    fun `WHEN invoked THEN it passes nickname, email and password through to the repository`() = runTest {
        val repository = FakeRegisterAccountRepository()
        val useCase = RegisterAccountUseCase(repository)

        useCase("nickname", "user@example.com", "password123")

        assertThat(repository.registerAccountInvocations).isEqualTo(
            mutableListOf(Triple("nickname", "user@example.com", "password123")),
        )
    }
}
