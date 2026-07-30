package com.croniot.client.features.sensors.presentation

import app.cash.turbine.test
import com.croniot.client.domain.models.ParameterSensor
import com.croniot.client.domain.models.SensorData
import com.croniot.client.domain.models.SensorType
import com.croniot.testing.fakes.FakeSensorDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SensorsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private fun sensorType(uid: Long) = SensorType(
        uid = uid,
        name = "sensor-$uid",
        description = "description-$uid",
        parameters = emptySet<ParameterSensor>(),
    )

    private fun sensorData(deviceUuid: String, sensorTypeUid: Long, value: String = "value") = SensorData(
        deviceUuid = deviceUuid,
        sensorTypeUid = sensorTypeUid,
        value = value,
        timeStamp = ZonedDateTime.now(),
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // loadAllInitialData fires its work on the real Dispatchers.IO (not swappable via Dispatchers.setMain),
    // so it does not complete synchronously within the test body. Instead of asserting right after calling
    // it (a latent race that existed before this migration too, with mocks or fakes alike), we await the
    // expected value on the StateFlow via Turbine, which suspends until the real background work lands.
    private suspend fun awaitState(
        viewModel: SensorsViewModel,
        expected: Map<Long, List<SensorData>>,
    ) {
        viewModel.sensorsInitialData.test(timeout = 5.seconds) {
            var latest = awaitItem()
            while (latest != expected) {
                latest = awaitItem()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadAllInitialData with two distinct SensorTypes queries the repository once per sensor and populates sensorsInitialData`() = runTest {
        val deviceUuid = "device-1"
        val temperatureType = sensorType(uid = 1L)
        val humidityType = sensorType(uid = 2L)
        val temperatureData = listOf(sensorData(deviceUuid, temperatureType.uid))
        val humidityData = listOf(sensorData(deviceUuid, humidityType.uid))
        val sensorDataRepository = FakeSensorDataRepository(
            latestSensorDataByKey = mapOf(
                (deviceUuid to temperatureType.uid) to temperatureData,
                (deviceUuid to humidityType.uid) to humidityData,
            ),
        )
        val viewModel = SensorsViewModel(sensorDataRepository)
        val expected = mapOf(
            temperatureType.uid to temperatureData,
            humidityType.uid to humidityData,
        )

        viewModel.loadAllInitialData(deviceUuid, listOf(temperatureType, humidityType))
        awaitState(viewModel, expected)

        assertEquals(1, sensorDataRepository.getLatestSensorDataInvocations.count { it == deviceUuid to temperatureType.uid })
        assertEquals(1, sensorDataRepository.getLatestSensorDataInvocations.count { it == deviceUuid to humidityType.uid })
    }

    @Test
    fun `loadAllInitialData called twice for the same deviceUuid and sensorType does not query the repository again`() = runTest {
        val deviceUuid = "device-1"
        val temperatureType = sensorType(uid = 1L)
        val temperatureData = listOf(sensorData(deviceUuid, temperatureType.uid))
        val sensorDataRepository = FakeSensorDataRepository(
            latestSensorDataByKey = mapOf((deviceUuid to temperatureType.uid) to temperatureData),
        )
        val viewModel = SensorsViewModel(sensorDataRepository)
        val expected = mapOf(temperatureType.uid to temperatureData)

        viewModel.loadAllInitialData(deviceUuid, listOf(temperatureType))
        awaitState(viewModel, expected) // ensures the cache is populated before the second call races it

        viewModel.loadAllInitialData(deviceUuid, listOf(temperatureType))

        assertEquals(1, sensorDataRepository.getLatestSensorDataInvocations.count { it == deviceUuid to temperatureType.uid })
    }

    @Test
    fun `listenSensorData called twice with the same deviceUuid and sensorUid returns the same StateFlow instance`() = runTest {
        val deviceUuid = "device-1"
        val sensorUid = 10L
        val liveData = MutableStateFlow(sensorData(deviceUuid, sensorUid))
        val sensorDataRepository = FakeSensorDataRepository(
            observeSensorDataByKey = mapOf((deviceUuid to sensorUid) to liveData),
        )
        val viewModel = SensorsViewModel(sensorDataRepository)

        val firstFlow = viewModel.listenSensorData(sensorUid, deviceUuid)
        val secondFlow = viewModel.listenSensorData(sensorUid, deviceUuid)

        assertSame(firstFlow, secondFlow)
        assertEquals(1, sensorDataRepository.observeSensorDataInvocations.count { it == deviceUuid to sensorUid })
    }

    @Test
    fun `listenSensorData with a different sensorUid or deviceUuid returns a different StateFlow instance`() = runTest {
        val deviceUuid = "device-1"
        val otherDeviceUuid = "device-2"
        val sensorUid = 10L
        val otherSensorUid = 20L
        val sensorDataRepository = FakeSensorDataRepository(
            observeSensorDataByKey = mapOf(
                (deviceUuid to sensorUid) to MutableStateFlow(sensorData(deviceUuid, sensorUid)),
                (deviceUuid to otherSensorUid) to MutableStateFlow(sensorData(deviceUuid, otherSensorUid)),
                (otherDeviceUuid to sensorUid) to MutableStateFlow(sensorData(otherDeviceUuid, sensorUid)),
            ),
        )
        val viewModel = SensorsViewModel(sensorDataRepository)

        val baseFlow = viewModel.listenSensorData(sensorUid, deviceUuid)
        val differentSensorFlow = viewModel.listenSensorData(otherSensorUid, deviceUuid)
        val differentDeviceFlow = viewModel.listenSensorData(sensorUid, otherDeviceUuid)

        assertNotSame(baseFlow, differentSensorFlow)
        assertNotSame(baseFlow, differentDeviceFlow)
    }
}
