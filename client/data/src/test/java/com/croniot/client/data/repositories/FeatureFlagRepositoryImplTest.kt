package com.croniot.client.data.repositories

import MqttHandler
import Outcome
import android.os.StrictMode
import android.util.Log
import com.croniot.client.data.source.local.FeatureFlagLocalDatasource
import com.croniot.client.data.source.local.ServerConfigLocalDatasource
import com.croniot.client.data.source.remote.http.FeatureFlagApi
import com.croniot.client.domain.models.FeatureFlagError
import croniot.messages.MessageFactory
import croniot.models.dto.FeatureFlagDto
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FeatureFlagRepositoryImplTest {

    private val api: FeatureFlagApi = mockk()
    private val localDatasource: FeatureFlagLocalDatasource = mockk()
    private val serverConfigLocalDatasource: ServerConfigLocalDatasource = mockk()
    private val appScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())
    private lateinit var repository: FeatureFlagRepositoryImpl

    private val flags = listOf(
        FeatureFlagDto(name = "flag-a", enabled = true),
        FeatureFlagDto(name = "flag-b", enabled = false),
    )

    @BeforeEach
    fun setUp() {
        repository = FeatureFlagRepositoryImpl(
            api = api,
            localDatasource = localDatasource,
            serverConfigLocalDatasource = serverConfigLocalDatasource,
            appScope = appScope,
        )
    }

    @Test
    fun `fetchAndCache returns Ok and saves flags to local datasource on success`() = runTest {
        coEvery { api.fetchAll() } returns flags
        coJustRun { localDatasource.saveFlags(any()) }

        val result = repository.fetchAndCache()

        assertEquals(Outcome.Ok(Unit), result)
        coVerify(exactly = 1) { localDatasource.saveFlags(flags) }
    }

    @Test
    fun `fetchAndCache populates flag cache so isEnabled reflects fetched flags`() = runTest {
        coEvery { api.fetchAll() } returns flags
        coJustRun { localDatasource.saveFlags(any()) }

        repository.fetchAndCache()

        assertTrue(repository.isEnabled("flag-a"))
        assertFalse(repository.isEnabled("flag-b"))
    }

    @Test
    fun `fetchAndCache returns Network error on HttpRequestTimeoutException`() = runTest {
        coEvery { api.fetchAll() } throws HttpRequestTimeoutException("http://example.com", 1000L)

        val result = repository.fetchAndCache()

        assertEquals(Outcome.Err(FeatureFlagError.Network), result)
    }

    @Test
    fun `fetchAndCache returns Network error on ConnectTimeoutException`() = runTest {
        coEvery { api.fetchAll() } throws ConnectTimeoutException("connect timeout")

        val result = repository.fetchAndCache()

        assertEquals(Outcome.Err(FeatureFlagError.Network), result)
    }

    @Test
    fun `fetchAndCache returns Network error on SocketTimeoutException`() = runTest {
        coEvery { api.fetchAll() } throws SocketTimeoutException("socket timeout")

        val result = repository.fetchAndCache()

        assertEquals(Outcome.Err(FeatureFlagError.Network), result)
    }

    @Test
    fun `fetchAndCache returns Network error on generic IOException`() = runTest {
        coEvery { api.fetchAll() } throws IOException("io error")

        val result = repository.fetchAndCache()

        assertEquals(Outcome.Err(FeatureFlagError.Network), result)
    }

    @Test
    fun `fetchAndCache returns Unknown error on unexpected exception`() = runTest {
        coEvery { api.fetchAll() } throws RuntimeException("boom")

        val result = repository.fetchAndCache()

        assertEquals(Outcome.Err(FeatureFlagError.Unknown), result)
    }

    @Test
    fun `fetchAndCache rethrows CancellationException instead of mapping it to Outcome`() = runTest {
        coEvery { api.fetchAll() } throws CancellationException("cancelled")

        var caught: CancellationException? = null
        try {
            repository.fetchAndCache()
        } catch (e: CancellationException) {
            caught = e
        }

        assertEquals("cancelled", caught?.message)
    }

    @Test
    fun `isEnabled returns false for a flag not present in cache`() = runTest {
        assertFalse(repository.isEnabled("unknown-flag"))
    }

    @Test
    fun `observeFlags populates the cache so isEnabled reflects emitted flags`() = runTest {
        coEvery { localDatasource.observeFlags() } returns kotlinx.coroutines.flow.flowOf(flags)

        repository.observeFlags().collect {}

        assertTrue(repository.isEnabled("flag-a"))
        assertFalse(repository.isEnabled("flag-b"))
    }

    @Test
    fun `observeFlags emits the same list of flags received from the datasource`() = runTest {
        coEvery { localDatasource.observeFlags() } returns kotlinx.coroutines.flow.flowOf(flags)

        val emitted = repository.observeFlags().first()

        assertEquals(flags, emitted)
    }

    // startMqttListener() runs its body in appScope.launch(Dispatchers.IO), a real dispatcher
    // untouched by the test's UnconfinedTestDispatcher, and it constructs a real MqttClient +
    // MqttHandler. MqttHandler's own init connects for real (out of scope, see MqttHandler
    // exclusion), so MqttClient/MqttHandler construction is stubbed to isolate the repository's
    // own branching (previous-handler disconnect, host fallback, flag-update forwarding).
    @AfterEach
    fun tearDownMqtt() {
        unmockkStatic(StrictMode::class)
        unmockkStatic(Log::class)
        unmockkConstructor(MqttClient::class, MqttHandler::class)
    }

    private fun stubMqttConstruction() {
        mockkConstructor(MqttClient::class, MqttHandler::class)
        every { anyConstructed<MqttClient>().connect(any<MqttConnectOptions>()) } just Runs
        every { anyConstructed<MqttClient>().setCallback(any<MqttCallback>()) } just Runs
        every { anyConstructed<MqttClient>().subscribe(any<String>(), any<Int>()) } just Runs
        every { anyConstructed<MqttClient>().unsubscribe(any<String>()) } just Runs
        every { anyConstructed<MqttClient>().disconnect() } just Runs
        every { anyConstructed<MqttClient>().close() } just Runs
        every { anyConstructed<MqttHandler>().disconnect() } just Runs
        mockkStatic(StrictMode::class)
        every { StrictMode.allowThreadDiskReads() } returns mockk(relaxed = true)
        every { StrictMode.setThreadPolicy(any()) } just Runs
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @Test
    fun `startMqttListener creates a handler using the default host when no server ip is stored`() = runTest {
        coEvery { serverConfigLocalDatasource.getServerIp() } returns flowOf(null)
        stubMqttConstruction()

        repository.startMqttListener()

        verify(timeout = 5000) { anyConstructed<MqttClient>().subscribe(any<String>(), any<Int>()) }
        verify(exactly = 0) { anyConstructed<MqttHandler>().disconnect() }
    }

    @Test
    fun `startMqttListener disconnects the previous handler when called again`() = runTest {
        coEvery { serverConfigLocalDatasource.getServerIp() } returns flowOf("10.0.0.5")
        stubMqttConstruction()

        repository.startMqttListener()
        verify(timeout = 5000) { anyConstructed<MqttClient>().subscribe(any<String>(), any<Int>()) }

        repository.startMqttListener()
        verify(timeout = 5000, exactly = 1) { anyConstructed<MqttHandler>().disconnect() }
    }

    @Test
    fun `startMqttListener forwards incoming flag updates to the cache and the local datasource`() = runTest {
        coEvery { serverConfigLocalDatasource.getServerIp() } returns flowOf("10.0.0.5")
        coJustRun { localDatasource.updateFlag(any(), any()) }
        stubMqttConstruction()
        val callbackSlot = slot<MqttCallback>()
        every { anyConstructed<MqttClient>().setCallback(capture(callbackSlot)) } just Runs

        repository.startMqttListener()
        verify(timeout = 5000) { anyConstructed<MqttClient>().subscribe(any<String>(), any<Int>()) }

        val flagJson = MessageFactory.toJson(FeatureFlagDto(name = "ignored", enabled = true))
        callbackSlot.captured.messageArrived("server/feature_flags/dark_mode", MqttMessage(flagJson.toByteArray()))

        assertTrue(repository.isEnabled("dark_mode"))
        coVerify(timeout = 5000) { localDatasource.updateFlag("dark_mode", true) }
    }
}
