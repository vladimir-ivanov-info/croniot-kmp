package com.croniot.client.data.repositories

import com.croniot.client.domain.models.Account
import com.croniot.client.data.source.local.LocalDatasource
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

    private val localDatasource: LocalDatasource = mockk()
    private lateinit var repository: AccountRepositoryImpl

    private val account = Account(
        uuid = "account-uuid",
        nickname = "nickname",
        email = "user@example.com",
        devices = emptyList(),
    )

    @BeforeEach
    fun setUp() {
        repository = AccountRepositoryImpl(localDatasource)
    }

    @Test
    fun `save delegates account to local datasource`() = runTest {
        coJustRun { localDatasource.saveCurrentAccount(any()) }

        repository.save(account)

        coVerify(exactly = 1) { localDatasource.saveCurrentAccount(account) }
    }

    @Test
    fun `get returns account from local datasource`() = runTest {
        coEvery { localDatasource.getCurrentAccount() } returns account

        val result = repository.get("user@example.com")

        assertEquals(account, result)
    }

    @Test
    fun `get returns null when no account stored`() = runTest {
        coEvery { localDatasource.getCurrentAccount() } returns null

        val result = repository.get("user@example.com")

        assertNull(result)
    }

    @Test
    fun `get ignores email parameter and delegates directly to datasource`() = runTest {
        coEvery { localDatasource.getCurrentAccount() } returns account

        repository.get("some-other-email@example.com")

        coVerify(exactly = 1) { localDatasource.getCurrentAccount() }
    }
}
