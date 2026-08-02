package com.server.croniot.data.db.daos

import com.server.croniot.data.db.entities.DeviceEntity
import com.server.croniot.testsupport.PostgresTestcontainer
import croniot.models.Account
import croniot.models.Device
import croniot.models.ParameterTask
import croniot.models.TaskType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TaskTypeDaoJooqImplIntegrationTest {

    private val accountDao = AccountJooqDaoImpl(PostgresTestcontainer.dsl)
    private val deviceDao = DeviceJooqDaoImpl(PostgresTestcontainer.dsl)
    private val dao = TaskTypeDaoJooqImpl(PostgresTestcontainer.dsl)

    @BeforeEach
    fun setUp() {
        PostgresTestcontainer.reset()
    }

    @Test
    fun `WHEN deviceId is zero THEN upsert throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            dao.upsert(taskType(uid = 1L), deviceId = 0L)
        }
    }

    @Test
    fun `WHEN task type is new THEN upsert inserts it with parameters and constraints`() {
        val deviceId = insertDevice()

        val id = dao.upsert(
            taskType(
                uid = 42L,
                name = "Rotate logs",
                parameters = listOf(
                    parameterTask(
                        uid = 1L,
                        name = "schedule",
                        constraints = mapOf("cron" to "0 * * * *"),
                    ),
                ),
            ),
            deviceId = deviceId,
        )

        assertTrue(id > 0)
        val stored = dao.getByDeviceIds(listOf(deviceId))[deviceId]!!.single()
        assertEquals("Rotate logs", stored.name)
        val param = stored.parameters.single()
        assertEquals("schedule", param.name)
        assertEquals(mapOf("cron" to "0 * * * *"), param.constraints)
    }

    @Test
    fun `WHEN a row already exists for that (device,uid) THEN upsert updates it and resets its constraints`() {
        val deviceId = insertDevice()
        val firstId = dao.upsert(
            taskType(
                uid = 42L,
                name = "initial",
                description = "v1",
                parameters = listOf(
                    parameterTask(uid = 1L, constraints = mapOf("old" to "x")),
                ),
            ),
            deviceId = deviceId,
        )

        val secondId = dao.upsert(
            taskType(
                uid = 42L,
                name = "updated",
                description = "v2",
                parameters = listOf(
                    parameterTask(uid = 1L, name = "p-updated", constraints = mapOf("new" to "z")),
                ),
            ),
            deviceId = deviceId,
        )

        assertEquals(firstId, secondId)
        val stored = dao.getByDeviceIds(listOf(deviceId))[deviceId]!!.single()
        assertEquals("updated", stored.name)
        assertEquals("v2", stored.description)
        val param = stored.parameters.single()
        assertEquals("p-updated", param.name)
        assertEquals(mapOf("new" to "z"), param.constraints)
    }

    @Test
    fun `WHEN device ids are given THEN getByDeviceIds groups results by deviceId and handles empty input`() {
        val deviceA = insertDevice(email = "a@example.com", deviceUuid = "dev-A")
        val deviceB = insertDevice(email = "b@example.com", deviceUuid = "dev-B")

        dao.upsert(taskType(uid = 1L, name = "A1"), deviceId = deviceA)
        dao.upsert(taskType(uid = 2L, name = "A2"), deviceId = deviceA)
        dao.upsert(taskType(uid = 3L, name = "B1"), deviceId = deviceB)

        val grouped = dao.getByDeviceIds(listOf(deviceA, deviceB))
        assertEquals(setOf("A1", "A2"), grouped[deviceA]!!.map { it.name }.toSet())
        assertEquals(listOf("B1"), grouped[deviceB]!!.map { it.name })

        assertTrue(dao.getByDeviceIds(emptyList()).isEmpty())
    }

    @Test
    fun `WHEN (device,uid) pair is unique THEN getId and exists reflect it`() {
        val deviceA = insertDevice(email = "a@example.com", deviceUuid = "dev-A")
        val deviceB = insertDevice(email = "b@example.com", deviceUuid = "dev-B")
        val idA = dao.upsert(taskType(uid = 99L, name = "A"), deviceId = deviceA)

        assertEquals(idA, dao.getId(deviceA, 99L))
        assertNull(dao.getId(deviceB, 99L), "uid 99 does not belong to deviceB")
        assertNull(dao.getId(deviceA, 100L), "deviceA has no task type with uid 100")

        assertTrue(dao.exists(taskTypeUid = 99L, deviceId = deviceA))
        assertFalse(dao.exists(taskTypeUid = 99L, deviceId = deviceB))
    }

    @Test
    fun `WHEN task type matches THEN getByDeviceUuid returns it, otherwise null`() {
        val deviceId = insertDevice(deviceUuid = "dev-X")
        dao.upsert(taskType(uid = 7L, name = "target"), deviceId = deviceId)

        val found = dao.getByDeviceUuid("dev-X", 7L)
        assertNotNull(found)
        assertEquals(7L, found!!.uid)
        assertEquals("target", found.name)

        assertNull(dao.getByDeviceUuid("dev-X", 999L))
        assertNull(dao.getByDeviceUuid("missing-uuid", 7L))
    }

    @Test
    fun `WHEN get and getLazy are called THEN they are stubs that currently return null`() {
        // Documented contract: both are TODO jOOQ implementations and return null today.
        val device = Device(uuid = "any", name = "n", description = "d", iot = false)
        assertNull(dao.get(device, 1L))
        assertNull(dao.getLazy(device, 1L))
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

    private fun taskType(
        uid: Long,
        name: String = "tt-$uid",
        description: String = "desc",
        parameters: List<ParameterTask> = emptyList(),
    ): TaskType = TaskType(
        uid = uid,
        name = name,
        description = description,
        parameters = parameters,
    )

    private fun parameterTask(
        uid: Long,
        name: String = "p-$uid",
        type: String = "string",
        unit: String = "",
        description: String = "",
        constraints: Map<String, String> = emptyMap(),
    ): ParameterTask = ParameterTask(
        uid = uid,
        name = name,
        type = type,
        unit = unit,
        description = description,
        constraints = constraints,
    )
}
