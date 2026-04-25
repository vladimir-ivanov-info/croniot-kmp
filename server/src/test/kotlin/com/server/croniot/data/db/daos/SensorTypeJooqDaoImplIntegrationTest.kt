package com.server.croniot.data.db.daos

import com.server.croniot.data.db.entities.DeviceEntity
import com.server.croniot.testsupport.PostgresTestcontainer
import croniot.models.Account
import croniot.models.ParameterSensor
import croniot.models.SensorType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SensorTypeJooqDaoImplIntegrationTest {

    private val accountDao = AccountJooqDaoImpl(PostgresTestcontainer.dsl)
    private val deviceDao = DeviceJooqDaoImpl(PostgresTestcontainer.dsl)
    private val dao = SensorTypeJooqDaoImpl(PostgresTestcontainer.dsl)

    @BeforeEach
    fun setUp() {
        PostgresTestcontainer.reset()
    }

    @Test
    fun `upsert requires a non-zero deviceId`() {
        assertThrows<IllegalArgumentException> {
            dao.upsert(sensorType(uid = 1L), deviceId = 0L)
        }
    }

    @Test
    fun `upsert inserts a new sensor type with its parameters and constraints`() {
        val deviceId = insertDevice()

        val id = dao.upsert(
            sensorType(
                uid = 100L,
                name = "Temperature",
                parameters = listOf(
                    parameterSensor(
                        uid = 1L,
                        name = "value",
                        type = "double",
                        unit = "C",
                        constraints = mapOf("minValue" to "-20", "maxValue" to "50"),
                    ),
                ),
            ),
            deviceId = deviceId,
        )

        assertTrue(id > 0)
        val grouped = dao.getByDeviceIds(listOf(deviceId))
        val stored = grouped[deviceId]?.single()
        assertNotNull(stored)
        assertEquals("Temperature", stored!!.name)
        val param = stored.parameters.single()
        assertEquals("value", param.name)
        assertEquals(mapOf("minValue" to "-20", "maxValue" to "50"), param.constraints)
    }

    @Test
    fun `upsert updates the existing sensor type on the same (device,uid) and resets its constraints`() {
        val deviceId = insertDevice()
        val firstId = dao.upsert(
            sensorType(
                uid = 42L,
                name = "initial",
                description = "v1",
                parameters = listOf(
                    parameterSensor(uid = 1L, constraints = mapOf("old" to "x", "keep" to "y")),
                ),
            ),
            deviceId = deviceId,
        )

        val secondId = dao.upsert(
            sensorType(
                uid = 42L,
                name = "updated",
                description = "v2",
                parameters = listOf(
                    parameterSensor(uid = 1L, name = "p-updated", constraints = mapOf("new" to "z")),
                ),
            ),
            deviceId = deviceId,
        )

        assertEquals(firstId, secondId, "must hit the same (device,uid) row, not create a new one")
        val stored = dao.getByDeviceIds(listOf(deviceId))[deviceId]!!.single()
        assertEquals("updated", stored.name)
        assertEquals("v2", stored.description)
        val param = stored.parameters.single()
        assertEquals("p-updated", param.name)
        // Old constraints must be wiped, new ones must be present.
        assertEquals(mapOf("new" to "z"), param.constraints)
    }

    @Test
    fun `upsert isolates sensor types across devices (same uid, different device)`() {
        val deviceA = insertDevice(email = "a@example.com", deviceUuid = "dev-A")
        val deviceB = insertDevice(email = "b@example.com", deviceUuid = "dev-B")

        val idA = dao.upsert(sensorType(uid = 7L, name = "A-sensor"), deviceId = deviceA)
        val idB = dao.upsert(sensorType(uid = 7L, name = "B-sensor"), deviceId = deviceB)

        // Different rows even though uid matches — the unique constraint is (device, uid).
        assertTrue(idA != idB)
    }

    @Test
    fun `getByDeviceIds groups results by deviceId and returns empty when no match`() {
        val deviceA = insertDevice(email = "a@example.com", deviceUuid = "dev-A")
        val deviceB = insertDevice(email = "b@example.com", deviceUuid = "dev-B")

        dao.upsert(sensorType(uid = 1L, name = "A1"), deviceId = deviceA)
        dao.upsert(sensorType(uid = 2L, name = "A2"), deviceId = deviceA)
        dao.upsert(sensorType(uid = 3L, name = "B1"), deviceId = deviceB)

        val grouped = dao.getByDeviceIds(listOf(deviceA, deviceB))
        assertEquals(setOf(deviceA, deviceB), grouped.keys)
        assertEquals(setOf("A1", "A2"), grouped[deviceA]!!.map { it.name }.toSet())
        assertEquals(listOf("B1"), grouped[deviceB]!!.map { it.name })

        assertTrue(dao.getByDeviceIds(emptyList()).isEmpty())
        assertTrue(dao.getByDeviceIds(listOf(99_999L)).isEmpty())
    }

    @Test
    fun `upsert drops blank-keyed constraints silently`() {
        val deviceId = insertDevice()
        dao.upsert(
            sensorType(
                uid = 1L,
                parameters = listOf(
                    parameterSensor(
                        uid = 1L,
                        constraints = mapOf("" to "ignored", "real" to "1"),
                    ),
                ),
            ),
            deviceId = deviceId,
        )

        val stored = dao.getByDeviceIds(listOf(deviceId))[deviceId]!!.single()
        assertEquals(mapOf("real" to "1"), stored.parameters.single().constraints)
    }

    private fun insertDevice(
        email: String = "user@example.com",
        deviceUuid: String = "device-uuid",
    ): Long {
        val accountId = accountDao.insert(
            Account(uuid = "uuid-$email", nickname = "nick", email = email, devices = mutableListOf()),
            password = "pwd",
        )
        return deviceDao.insert(
            DeviceEntity(
                uuid = deviceUuid,
                name = "name",
                description = "desc",
                iot = false,
                accountId = accountId,
            ),
        )
    }

    private fun sensorType(
        uid: Long,
        name: String = "sensor-$uid",
        description: String = "desc",
        parameters: List<ParameterSensor> = emptyList(),
    ): SensorType = SensorType(
        uid = uid,
        name = name,
        description = description,
        parameters = parameters,
    )

    private fun parameterSensor(
        uid: Long,
        name: String = "p-$uid",
        type: String = "double",
        unit: String = "u",
        description: String = "",
        constraints: Map<String, String> = emptyMap(),
    ): ParameterSensor = ParameterSensor(
        uid = uid,
        name = name,
        type = type,
        unit = unit,
        description = description,
        constraints = constraints,
    )
}
