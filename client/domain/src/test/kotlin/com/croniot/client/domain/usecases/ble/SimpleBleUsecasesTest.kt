package com.croniot.client.domain.usecases.ble

import Outcome
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.croniot.client.domain.models.Device
import com.croniot.testing.fakes.FakeBleDevicesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SimpleBleUsecasesTest {

    private val device = Device(uuid = "device-1", name = "Device 1", description = "")

    @Test
    fun `ScanBleDevicesUseCase delegates to observeNearbyDevices`() = runTest {
        val repository = FakeBleDevicesRepository()
        val useCase = ScanBleDevicesUseCase(repository)

        val result = useCase().first()

        assertThat(result).isEqualTo(emptyList())
    }

    @Test
    fun `ObserveKnownBleDevicesUseCase delegates to observeKnownDevices`() = runTest {
        val repository = FakeBleDevicesRepository()
        val useCase = ObserveKnownBleDevicesUseCase(repository)

        val result = useCase().first()

        assertThat(result).isEqualTo(emptyList())
    }

    @Test
    fun `ObserveBleRssiUseCase delegates to observeRssi for given device`() = runTest {
        val repository = FakeBleDevicesRepository()
        val useCase = ObserveBleRssiUseCase(repository)

        val result = useCase("device-1").first()

        assertThat(result).isEqualTo(null)
    }

    @Test
    fun `PairBleDeviceUseCase returns device on success`() = runTest {
        val repository = FakeBleDevicesRepository(devicesByUuid = mapOf("device-1" to device))
        val useCase = PairBleDeviceUseCase(repository)

        val result = useCase("device-1", "user", "pass")

        assertThat(result).isEqualTo(Outcome.Ok(device))
    }

    @Test
    fun `PairBleDeviceUseCase returns error when device is unknown`() = runTest {
        val repository = FakeBleDevicesRepository()
        val useCase = PairBleDeviceUseCase(repository)

        val result = useCase("unknown-device", "user", "pass")

        assertThat(result).isInstanceOf(Outcome.Err::class)
    }

    @Test
    fun `ConnectBleDeviceUseCase returns device on success`() = runTest {
        val repository = FakeBleDevicesRepository(devicesByUuid = mapOf("device-1" to device))
        val useCase = ConnectBleDeviceUseCase(repository)

        val result = useCase("device-1")

        assertThat(result).isEqualTo(Outcome.Ok(device))
    }

    @Test
    fun `ConnectBleDeviceUseCase returns error when device is unknown`() = runTest {
        val repository = FakeBleDevicesRepository()
        val useCase = ConnectBleDeviceUseCase(repository)

        val result = useCase("unknown-device")

        assertThat(result).isInstanceOf(Outcome.Err::class)
    }

    @Test
    fun `ForgetBleDeviceUseCase removes the device from the repository`() = runTest {
        val repository = FakeBleDevicesRepository(devicesByUuid = mapOf("device-1" to device))
        val useCase = ForgetBleDeviceUseCase(repository)

        useCase("device-1")

        assertThat(repository.forgetCalls).isEqualTo(1)
        assertThat(repository.getDevice("device-1")).isEqualTo(null)
    }
}
