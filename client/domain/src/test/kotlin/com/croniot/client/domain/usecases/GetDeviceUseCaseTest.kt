package com.croniot.client.domain.usecases

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.croniot.client.domain.models.Account
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.session.AppSession
import com.croniot.testing.fakes.FakeAppSessionRepository
import com.croniot.testing.fakes.FakeBleDevicesRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class GetDeviceUseCaseTest {

    private val matchingDevice = Device(uuid = "device-1", name = "Device 1", description = "")
    private val otherDevice = Device(uuid = "device-2", name = "Device 2", description = "")

    @Test
    fun `returns matching device from the server session account`() = runTest {
        val account = Account(
            uuid = "account-1",
            nickname = "nick",
            email = "user@example.com",
            devices = listOf(matchingDevice, otherDevice),
        )
        val bleDevicesRepository = FakeBleDevicesRepository()
        val useCase = buildUseCase(
            appSessionRepository = FakeAppSessionRepository(initial = AppSession.Server(account)),
            bleDevicesRepository = bleDevicesRepository,
        )

        val result = useCase(deviceUuid = "device-1")

        assertThat(result).isEqualTo(matchingDevice)
        assertThat(bleDevicesRepository.getDeviceInvocations).isEqualTo(mutableListOf())
    }

    @Test
    fun `returns null when device is not part of the server session account`() = runTest {
        val account = Account(
            uuid = "account-1",
            nickname = "nick",
            email = "user@example.com",
            devices = listOf(otherDevice),
        )
        val useCase = buildUseCase(
            appSessionRepository = FakeAppSessionRepository(initial = AppSession.Server(account)),
        )

        val result = useCase(deviceUuid = "device-1")

        assertThat(result).isNull()
    }

    @Test
    fun `delegates to ble devices repository when in ble only mode`() = runTest {
        val bleDevicesRepository = FakeBleDevicesRepository(devicesByUuid = mapOf("device-1" to matchingDevice))
        val useCase = buildUseCase(
            appSessionRepository = FakeAppSessionRepository(initial = AppSession.BleOnly),
            bleDevicesRepository = bleDevicesRepository,
        )

        val result = useCase(deviceUuid = "device-1")

        assertThat(result).isEqualTo(matchingDevice)
        assertThat(bleDevicesRepository.getDeviceInvocations).isEqualTo(mutableListOf("device-1"))
    }

    @Test
    fun `returns null without querying ble repository when there is no session`() = runTest {
        val bleDevicesRepository = FakeBleDevicesRepository()
        val useCase = buildUseCase(
            appSessionRepository = FakeAppSessionRepository(initial = AppSession.None),
            bleDevicesRepository = bleDevicesRepository,
        )

        val result = useCase(deviceUuid = "device-1")

        assertThat(result).isNull()
        assertThat(bleDevicesRepository.getDeviceInvocations).isEqualTo(mutableListOf())
    }

    private fun buildUseCase(
        appSessionRepository: FakeAppSessionRepository = FakeAppSessionRepository(),
        bleDevicesRepository: FakeBleDevicesRepository = FakeBleDevicesRepository(),
    ) = GetDeviceUseCase(
        appSessionRepository = appSessionRepository,
        bleDevicesRepository = bleDevicesRepository,
    )
}
