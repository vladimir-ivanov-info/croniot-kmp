package com.server.croniot.data.db.daos

import com.server.croniot.data.db.entities.DeviceEntity
import com.server.croniot.testsupport.PostgresTestcontainer
import croniot.models.Account
import croniot.models.DeviceToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DeviceTokenJooqDaoImplIntegrationTest {

    private val accountDao = AccountJooqDaoImpl(PostgresTestcontainer.dsl)
    private val deviceDao = DeviceJooqDaoImpl(PostgresTestcontainer.dsl)
    private val dao = DeviceTokenJooqDaoImpl(PostgresTestcontainer.dsl)

    @BeforeEach
    fun setUp() {
        PostgresTestcontainer.reset()
    }

    @Test
    fun `insert requires a non-zero deviceId`() {
        assertThrows<IllegalArgumentException> {
            dao.insert(DeviceToken(deviceId = 0L, token = "any"))
        }
    }

    @Test
    fun `getDeviceAssociatedWithToken returns the joined device and null for unknown token`() {
        val (_, deviceId) = insertAccountAndDevice(deviceUuid = "device-X")
        dao.insert(DeviceToken(deviceId = deviceId, token = "tok-X"))

        val found = dao.getDeviceAssociatedWithToken("tok-X")
        assertNotNull(found)
        assertEquals("device-X", found!!.uuid)

        assertNull(dao.getDeviceAssociatedWithToken("missing-token"))
    }

    @Test
    fun `getDeviceUuidAssociatedWithToken returns uuid when token exists and null otherwise`() {
        val (_, deviceId) = insertAccountAndDevice(deviceUuid = "device-Y")
        dao.insert(DeviceToken(deviceId = deviceId, token = "tok-Y"))

        assertEquals("device-Y", dao.getDeviceUuidAssociatedWithToken("tok-Y"))
        assertNull(dao.getDeviceUuidAssociatedWithToken("nope"))
    }

    @Test
    fun `isTokenCorrect returns true only when device uuid and token match in the same row`() {
        val (_, deviceA) = insertAccountAndDevice(deviceUuid = "device-A")
        val (_, deviceB) = insertAccountAndDevice(
            email = "b@example.com",
            deviceUuid = "device-B",
        )
        dao.insert(DeviceToken(deviceId = deviceA, token = "tok-A"))
        dao.insert(DeviceToken(deviceId = deviceB, token = "tok-B"))

        assertTrue(dao.isTokenCorrect("device-A", "tok-A"))
        // Cross pair must NOT be accepted — the row linking A to tok-A must not authorize device-B.
        assertFalse(dao.isTokenCorrect("device-A", "tok-B"))
        assertFalse(dao.isTokenCorrect("device-B", "tok-A"))
        assertFalse(dao.isTokenCorrect("missing-uuid", "tok-A"))
    }

    private fun insertAccountAndDevice(
        email: String = "user@example.com",
        deviceUuid: String = "device-uuid",
    ): Pair<Long, Long> {
        val accountId = accountDao.insert(
            Account(uuid = "uuid-$email", nickname = "nick", email = email, devices = mutableListOf()),
            password = "pwd",
        )
        val deviceId = deviceDao.insert(
            DeviceEntity(
                uuid = deviceUuid,
                name = "name",
                description = "desc",
                iot = false,
                accountId = accountId,
            ),
        )
        return accountId to deviceId
    }
}
