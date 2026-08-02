package com.croniot.client.data.repositories

import com.croniot.client.domain.models.Account
import com.croniot.client.domain.models.session.AppSession
import com.croniot.client.domain.repositories.LocalDataRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppSessionRepositoryImplTest {

    private val localDataRepository: LocalDataRepository = mockk()
    private val account = Account(uuid = "account-1", email = "user@example.com", nickname = "user", devices = emptyList())

    private fun newRepository(scope: kotlinx.coroutines.CoroutineScope) =
        AppSessionRepositoryImpl(localDataRepository, scope)

    @Test
    fun `WHEN no session mode is stored THEN initial session is None`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { localDataRepository.getAppSessionMode() } returns null

        val repository = newRepository(this)

        assertEquals(AppSession.None, repository.session.value)
    }

    @Test
    fun `WHEN mode is server and account exists THEN initial session is Server with account`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { localDataRepository.getAppSessionMode() } returns "server"
            coEvery { localDataRepository.getCurrentAccount() } returns account

            val repository = newRepository(this)

            assertEquals(AppSession.Server(account), repository.session.value)
        }

    @Test
    fun `WHEN mode is server but account is missing THEN initial session is None`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { localDataRepository.getAppSessionMode() } returns "server"
            coEvery { localDataRepository.getCurrentAccount() } returns null

            val repository = newRepository(this)

            assertEquals(AppSession.None, repository.session.value)
        }

    @Test
    fun `WHEN mode is ble THEN initial session is BleOnly`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { localDataRepository.getAppSessionMode() } returns "ble"

        val repository = newRepository(this)

        assertEquals(AppSession.BleOnly, repository.session.value)
    }

    @Test
    fun `WHEN activateServerSession is called THEN it persists server mode and updates session to Server`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { localDataRepository.getAppSessionMode() } returns null
            coEvery { localDataRepository.saveAppSessionMode("server") } returns Unit
            val repository = newRepository(this)

            repository.activateServerSession(account)

            assertEquals(AppSession.Server(account), repository.session.value)
            coVerify(exactly = 1) { localDataRepository.saveAppSessionMode("server") }
        }

    @Test
    fun `WHEN activateBleOnlyMode is called THEN it persists ble mode and updates session to BleOnly`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { localDataRepository.getAppSessionMode() } returns null
            coEvery { localDataRepository.saveAppSessionMode("ble") } returns Unit
            val repository = newRepository(this)

            repository.activateBleOnlyMode()

            assertEquals(AppSession.BleOnly, repository.session.value)
            coVerify(exactly = 1) { localDataRepository.saveAppSessionMode("ble") }
        }

    @Test
    fun `WHEN clear is called THEN it persists null mode and resets session to None`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { localDataRepository.getAppSessionMode() } returns "server"
        coEvery { localDataRepository.getCurrentAccount() } returns account
        coEvery { localDataRepository.saveAppSessionMode(null) } returns Unit
        val repository = newRepository(this)

        repository.clear()

        assertEquals(AppSession.None, repository.session.value)
        coVerify(exactly = 1) { localDataRepository.saveAppSessionMode(null) }
    }
}
