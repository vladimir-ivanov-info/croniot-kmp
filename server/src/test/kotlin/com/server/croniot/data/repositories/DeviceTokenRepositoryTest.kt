package com.server.croniot.data.repositories

import com.server.croniot.testsupport.fakes.FakeDeviceTokenDao
import croniot.models.Device
import croniot.models.DeviceToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DeviceTokenRepository] using a fake (not a mock) of
 * [com.server.croniot.data.db.daos.DeviceTokenDao]. [DeviceTokenRepository.createDeviceToken] builds
 * a [DeviceToken] domain object out of raw primitives before persisting it, which is worth
 * verifying explicitly; the remaining methods are plain delegation, still covered against the
 * fake's realistic lookup behavior.
 */
class DeviceTokenRepositoryTest {

    private val deviceTokenDao = FakeDeviceTokenDao()
    private val repository = DeviceTokenRepository(deviceTokenDao)

    @Test
    fun `createDeviceToken builds and persists a device token from the raw device id and token`() {
        repository.createDeviceToken(deviceId = 5L, token = "tok-123")

        assertEquals(listOf(DeviceToken(deviceId = 5L, token = "tok-123")), deviceTokenDao.insertedTokens)
    }

    @Test
    fun `getDevice returns the associated device for a known token and null otherwise`() {
        val device = Device(uuid = "device-uuid", name = "Device", iot = true)
        deviceTokenDao.seed("tok-123", device)

        assertEquals(device, repository.getDevice("tok-123"))
        assertNull(repository.getDevice("missing-token"))
    }

    @Test
    fun `getDeviceUuid returns the associated device uuid for a known token and null otherwise`() {
        deviceTokenDao.seedValidToken(deviceUuid = "device-uuid", token = "tok-123")

        assertEquals("device-uuid", repository.getDeviceUuid("tok-123"))
        assertNull(repository.getDeviceUuid("missing-token"))
    }

    @Test
    fun `isTokenCorrect is true only for the matching device uuid and token pair`() {
        deviceTokenDao.seedValidToken(deviceUuid = "device-uuid", token = "tok-123")

        assertTrue(repository.isTokenCorrect("device-uuid", "tok-123"))
        assertFalse(repository.isTokenCorrect("device-uuid", "wrong-token"))
        assertFalse(repository.isTokenCorrect("other-device", "tok-123"))
    }
}
