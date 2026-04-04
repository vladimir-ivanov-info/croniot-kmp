package com.croniot.client.data.repositories

import com.croniot.client.domain.models.auth.AuthSession
import com.croniot.client.data.source.local.LocalDatasource
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SessionRepositoryImplTest {

    private val localDatasource: LocalDatasource = mockk()
    private lateinit var repository: SessionRepositoryImpl

    @BeforeEach
    fun setUp() {
        repository = SessionRepositoryImpl(localDatasource)
    }

    @Test
    fun `save delegates email and token to local datasource`() = runTest {
        val session = AuthSession(email = "user@example.com", token = "token-123")
        coJustRun { localDatasource.saveEmail(any()) }
        coJustRun { localDatasource.saveToken(any()) }

        repository.save(session)

        coVerify(exactly = 1) { localDatasource.saveEmail("user@example.com") }
        coVerify(exactly = 1) { localDatasource.saveToken("token-123") }
    }

    @Test
    fun `clearAllExceptDeviceUuid delegates to local datasource`() = runTest {
        coJustRun { localDatasource.clearAllCacheExceptDeviceUuid() }

        repository.clearAllExceptDeviceUuid()

        coVerify(exactly = 1) { localDatasource.clearAllCacheExceptDeviceUuid() }
    }
}
