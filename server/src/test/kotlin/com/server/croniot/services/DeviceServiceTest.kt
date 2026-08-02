package com.server.croniot.services

import com.server.croniot.data.repositories.AccountRepository
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.DeviceTokenRepository
import croniot.messages.MessageRegisterDevice
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val deviceRepository: DeviceRepository = mockk()
    private val deviceTokenRepository: DeviceTokenRepository = mockk(relaxUnitFun = true)

    private val service = DeviceService(
        accountRepository = accountRepository,
        deviceRepository = deviceRepository,
        deviceTokenRepository = deviceTokenRepository,
    )

    private val message = MessageRegisterDevice(
        accountEmail = "user@example.com",
        accountPassword = "secret",
        deviceUuid = "device-uuid",
        deviceName = "Device",
        deviceDescription = "A device",
    )

    @Test
    fun `WHEN account does not exist THEN registerDevice returns failure`() {
        every { accountRepository.isAccountExists("user@example.com") } returns false

        val result = service.registerDevice(message)

        assertFalse(result.success)
        assertEquals("Account for user@example.com doesn't exist.", result.message)
        verify(exactly = 0) { deviceRepository.createDevice(any(), any()) }
    }

    @Test
    fun `WHEN account id cannot be resolved THEN registerDevice returns failure`() {
        every { accountRepository.isAccountExists("user@example.com") } returns true
        every { accountRepository.getAccountId("user@example.com") } returns null

        val result = service.registerDevice(message)

        assertFalse(result.success)
        assertEquals("Account for user@example.com doesn't exist.", result.message)
        verify(exactly = 0) { deviceRepository.createDevice(any(), any()) }
    }

    @Test
    fun `WHEN registration succeeds THEN registerDevice creates the device and a fresh token`() {
        every { accountRepository.isAccountExists("user@example.com") } returns true
        every { accountRepository.getAccountId("user@example.com") } returns 10L
        every { deviceRepository.createDevice(any(), 10L) } returns 99L
        val tokenSlot = slot<String>()

        val result = service.registerDevice(message)

        assertTrue(result.success)
        assertTrue(result.message.isNotBlank())
        verify(exactly = 1) { deviceRepository.createDevice(any(), 10L) }
        verify(exactly = 1) { deviceTokenRepository.createDeviceToken(99L, capture(tokenSlot)) }
        assertEquals(tokenSlot.captured, result.message)
    }

    @Test
    fun `WHEN an exception is thrown THEN registerDevice returns a success-flagged failure message`() {
        every { accountRepository.isAccountExists("user@example.com") } returns true
        every { accountRepository.getAccountId("user@example.com") } returns 10L
        every { deviceRepository.createDevice(any(), 10L) } throws RuntimeException("already exists")

        val result = service.registerDevice(message)

        // Note: current implementation swallows the exception and reports success = true,
        // conveying the failure only through the message text.
        assertTrue(result.success)
        assertEquals("Could not register device, probably it already exists.", result.message)
    }
}
