package com.croniot.client.domain.usecases

import Outcome
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.croniot.client.domain.models.ConnectionError
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.TaskType
import com.croniot.testing.fakes.FakeFeatureFlagRepository
import com.croniot.testing.fakes.FakeSensorDataRepository
import com.croniot.testing.fakes.FakeTaskTypesRepository
import com.croniot.testing.fakes.FakeTasksRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class StartDeviceListenersUseCaseTest {

    private fun buildUseCase(
        sensorDataRepository: FakeSensorDataRepository = FakeSensorDataRepository(),
        tasksRepository: FakeTasksRepository = FakeTasksRepository(),
        taskTypesRepository: FakeTaskTypesRepository = FakeTaskTypesRepository(),
        featureFlagRepository: FakeFeatureFlagRepository = FakeFeatureFlagRepository(),
    ) = StartDeviceListenersUseCase(
        sensorDataRepository = sensorDataRepository,
        tasksRepository = tasksRepository,
        taskTypesRepository = taskTypesRepository,
        featureFlagRepository = featureFlagRepository,
        fetchFeatureFlagsUseCase = FetchFeatureFlagsUseCase(featureFlagRepository),
    )

    @Test
    fun `WHEN all devices start listening successfully THEN it returns Ok`() = runTest {
        val device = Device(uuid = "device-1", name = "Device", description = "")
        val useCase = buildUseCase()

        val result = useCase(listOf(device))

        assertThat(result).isEqualTo(Outcome.Ok(Unit))
    }

    @Test
    fun `WHEN a device uuid starts with android THEN it is filtered out`() = runTest {
        val androidDevice = Device(uuid = "android-fake-1", name = "Fake", description = "")
        val realDevice = Device(uuid = "device-1", name = "Real", description = "")
        val sensorRepo = FakeSensorDataRepository()
        val useCase = buildUseCase(sensorDataRepository = sensorRepo)

        useCase(listOf(androidDevice, realDevice))

        assertThat(sensorRepo.listenToDeviceSensorsInvocations).isEqualTo(mutableListOf("device-1"))
    }

    @Test
    fun `WHEN invoked with devices and their task types THEN it registers all task types for each device`() = runTest {
        val taskType1 = TaskType(uid = 1L, name = "Water", description = "", parameters = emptyList())
        val taskType2 = TaskType(uid = 2L, name = "Light", description = "", parameters = emptyList())
        val device = Device(uuid = "device-1", name = "Device", description = "", taskTypes = listOf(taskType1, taskType2))
        val taskTypesRepo = FakeTaskTypesRepository()
        val useCase = buildUseCase(taskTypesRepository = taskTypesRepo)

        useCase(listOf(device))

        assertThat(taskTypesRepo.getAll("device-1")).isEqualTo(listOf(taskType1, taskType2))
    }

    @Test
    fun `WHEN the use case is invoked THEN it fetches and caches feature flags before starting listeners`() = runTest {
        val featureFlagRepo = FakeFeatureFlagRepository()
        val useCase = buildUseCase(featureFlagRepository = featureFlagRepo)

        useCase(emptyList())

        assertThat(featureFlagRepo.fetchAndCacheCalls).isEqualTo(1)
    }

    @Test
    fun `WHEN the use case is invoked THEN it starts the mqtt listener for feature flags`() = runTest {
        val featureFlagRepo = FakeFeatureFlagRepository()
        val useCase = buildUseCase(featureFlagRepository = featureFlagRepo)

        useCase(emptyList())

        assertThat(featureFlagRepo.startMqttListenerCalls).isEqualTo(1)
    }

    @Test
    fun `WHEN a device fails to start listening THEN it returns errors for that device`() = runTest {
        val device = Device(uuid = "device-1", name = "Device", description = "")
        val sensorRepo = FakeSensorDataRepository(
            listenToDeviceSensorsOutcomeByDevice = mapOf("device-1" to Outcome.Err(ConnectionError.Unknown)),
        )
        val useCase = buildUseCase(sensorDataRepository = sensorRepo)

        val result = useCase(listOf(device))

        assertThat(result).isEqualTo(Outcome.Err(listOf(ConnectionError.Unknown)))
    }

    @Test
    fun `WHEN device list is empty THEN it succeeds`() = runTest {
        val useCase = buildUseCase()

        val result = useCase(emptyList())

        assertThat(result).isEqualTo(Outcome.Ok(Unit))
    }

    @Test
    fun `WHEN starting a device throws an unexpected exception THEN it is reported as ConnectionError_Unknown instead of crashing`() = runTest {
        val goodDevice = Device(uuid = "device-good", name = "Good", description = "")
        val crashingDevice = Device(uuid = "device-crash", name = "Crash", description = "")
        val sensorRepo = FakeSensorDataRepository(
            listenToDeviceSensorsThrowsByDevice = mapOf("device-crash" to RuntimeException("boom")),
        )
        val useCase = buildUseCase(sensorDataRepository = sensorRepo)

        val result = useCase(listOf(goodDevice, crashingDevice))

        assertThat(result).isEqualTo(Outcome.Err(listOf(ConnectionError.Unknown)))
    }

    @Test
    fun `WHEN one device fails and another succeeds THEN it reports errors only for the failing device`() = runTest {
        val goodDevice = Device(uuid = "device-good", name = "Good", description = "")
        val badDevice = Device(uuid = "device-bad", name = "Bad", description = "")
        val sensorRepo = FakeSensorDataRepository(
            listenToDeviceSensorsOutcomeByDevice = mapOf(
                "device-bad" to Outcome.Err(ConnectionError.MqttBrokerUnreachable(host = "1.2.3.4", cause = "timeout")),
            ),
        )
        val useCase = buildUseCase(sensorDataRepository = sensorRepo)

        val result = useCase(listOf(goodDevice, badDevice))

        assertThat(result).isEqualTo(
            Outcome.Err(listOf(ConnectionError.MqttBrokerUnreachable(host = "1.2.3.4", cause = "timeout"))),
        )
    }
}
