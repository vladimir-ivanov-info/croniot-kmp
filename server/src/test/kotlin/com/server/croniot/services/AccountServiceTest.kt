package com.server.croniot.services

import com.server.croniot.application.DomainException
import com.server.croniot.data.repositories.AccountRepository
import croniot.messages.MessageRegisterAccount
import croniot.models.errors.DomainError
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccountServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val service = AccountService(accountRepository)

    private val message = MessageRegisterAccount(
        accountUuid = "acc-uuid",
        nickname = "nick",
        email = "user@example.com",
        password = "secret",
    )

    @Test
    fun `registerAccount persists account and returns success when email is available`() {
        every { accountRepository.isEmailAvailable("user@example.com") } returns true
        every { accountRepository.createAccount(any(), any(), any(), any()) } returns 1L

        val result = service.registerAccount(message)

        assertTrue(result.success)
        verify(exactly = 1) {
            accountRepository.createAccount("acc-uuid", "nick", "user@example.com", "secret")
        }
    }

    @Test
    fun `registerAccount throws Conflict and skips createAccount when email is taken`() {
        every { accountRepository.isEmailAvailable("user@example.com") } returns false

        val ex = assertThrows(DomainException::class.java) { service.registerAccount(message) }
        assertInstanceOf(DomainError.Conflict::class.java, ex.error)
        assertEquals("This email is already used.", ex.error.message)
        verify(exactly = 0) { accountRepository.createAccount(any(), any(), any(), any()) }
    }
}
