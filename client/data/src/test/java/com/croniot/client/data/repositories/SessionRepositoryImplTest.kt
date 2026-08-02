package com.croniot.client.data.repositories

import com.croniot.client.domain.models.auth.AuthSession
import com.croniot.client.domain.models.auth.AuthTokens
import com.croniot.client.data.source.local.AppPreferencesLocalDatasource
import com.croniot.client.data.source.local.AuthLocalDatasource
import com.croniot.client.data.source.local.TokenStore
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SessionRepositoryImplTest {

    private val authLocalDatasource: AuthLocalDatasource = mockk()
    private val appPreferencesLocalDatasource: AppPreferencesLocalDatasource = mockk()
    private val tokenStore: TokenStore = mockk()
    private lateinit var repository: SessionRepositoryImpl

    @BeforeEach
    fun setUp() {
        repository = SessionRepositoryImpl(
            authLocalDatasource = authLocalDatasource,
            appPreferencesLocalDatasource = appPreferencesLocalDatasource,
            tokenStore = tokenStore,
        )
    }

    @Test
    fun `WHEN save is called THEN it delegates email to auth datasource`() = runTest {
        val session = AuthSession(email = "user@example.com", token = "token-123")
        coJustRun { authLocalDatasource.saveEmail(any()) }

        repository.save(session)

        coVerify(exactly = 1) { authLocalDatasource.saveEmail("user@example.com") }
    }

    @Test
    fun `WHEN saveTokens is called THEN it delegates to token store`() = runTest {
        val tokens = AuthTokens(
            accessToken = "access-123",
            refreshToken = "refresh-123",
            expiresAtEpochSeconds = 1_700_000_000L,
        )
        coJustRun { tokenStore.saveTokens(any()) }

        repository.saveTokens(tokens)

        coVerify(exactly = 1) { tokenStore.saveTokens(tokens) }
    }

    @Test
    fun `WHEN clearAllExceptDeviceUuid is called THEN it clears tokens and app preferences`() = runTest {
        coJustRun { tokenStore.clearTokens() }
        coJustRun { appPreferencesLocalDatasource.clearAllCacheExceptDeviceUuid() }

        repository.clearAllExceptDeviceUuid()

        coVerify(exactly = 1) { tokenStore.clearTokens() }
        coVerify(exactly = 1) { appPreferencesLocalDatasource.clearAllCacheExceptDeviceUuid() }
    }

    @Test
    fun `WHEN token store has tokens THEN getTokens returns them`() = runTest {
        val tokens = AuthTokens(
            accessToken = "access-123",
            refreshToken = "refresh-123",
            expiresAtEpochSeconds = 1_700_000_000L,
        )
        coEvery { tokenStore.getTokens() } returns tokens

        val result = repository.getTokens()

        assertEquals(tokens, result)
    }

    @Test
    fun `WHEN token store has none THEN getTokens returns null`() = runTest {
        coEvery { tokenStore.getTokens() } returns null

        val result = repository.getTokens()

        assertNull(result)
    }

    @Test
    fun `WHEN clearTokens is called THEN it delegates to token store`() = runTest {
        coJustRun { tokenStore.clearTokens() }

        repository.clearTokens()

        coVerify(exactly = 1) { tokenStore.clearTokens() }
    }
}
