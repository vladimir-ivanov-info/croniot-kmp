package com.croniot.client.data.repositories

import com.croniot.client.domain.models.Account
import com.croniot.client.data.source.local.AuthLocalDatasource
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AccountRepositoryImplTest {

    private val authLocalDatasource: AuthLocalDatasource = mockk()
    private lateinit var repository: AccountRepositoryImpl

    private val account = Account(
        uuid = "account-uuid",
        nickname = "nickname",
        email = "user@example.com",
        devices = emptyList(),
    )

    @BeforeEach
    fun setUp() {
        repository = AccountRepositoryImpl(authLocalDatasource)
    }

    @Test
    fun `WHEN save is called THEN it delegates account to local datasource`() = runTest {
        coJustRun { authLocalDatasource.saveCurrentAccount(any()) }

        repository.save(account)

        coVerify(exactly = 1) { authLocalDatasource.saveCurrentAccount(account) }
    }

    @Test
    fun `WHEN local datasource has an account THEN get returns it`() = runTest {
        coEvery { authLocalDatasource.getCurrentAccount() } returns account

        val result = repository.get("user@example.com")

        assertEquals(account, result)
    }

    @Test
    fun `WHEN no account is stored THEN get returns null`() = runTest {
        coEvery { authLocalDatasource.getCurrentAccount() } returns null

        val result = repository.get("user@example.com")

        assertNull(result)
    }

    @Test
    fun `WHEN get is called THEN it ignores the email parameter and delegates directly to datasource`() = runTest {
        coEvery { authLocalDatasource.getCurrentAccount() } returns account

        repository.get("some-other-email@example.com")

        coVerify(exactly = 1) { authLocalDatasource.getCurrentAccount() }
    }
}
