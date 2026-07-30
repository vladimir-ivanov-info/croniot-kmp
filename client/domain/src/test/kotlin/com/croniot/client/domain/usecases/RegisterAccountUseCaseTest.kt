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
    fun `returns success result from repository`() = runTest {
        val repository = FakeRegisterAccountRepository(result = Result(success = true, message = ""))
        val useCase = RegisterAccountUseCase(repository)

        val result = useCase("nickname", "user@example.com", "password123")

        assertThat(result.success).isTrue()
    }

    @Test
    fun `returns failure result from repository`() = runTest {
        val repository = FakeRegisterAccountRepository(result = Result(success = false, message = "Email taken"))
        val useCase = RegisterAccountUseCase(repository)

        val result = useCase("nickname", "user@example.com", "password123")

        assertThat(result.success).isFalse()
        assertThat(result.message).isEqualTo("Email taken")
    }

    @Test
    fun `passes through nickname email and password to repository`() = runTest {
        val repository = FakeRegisterAccountRepository()
        val useCase = RegisterAccountUseCase(repository)

        useCase("nickname", "user@example.com", "password123")

        assertThat(repository.registerAccountInvocations).isEqualTo(
            mutableListOf(Triple("nickname", "user@example.com", "password123")),
        )
    }
}
