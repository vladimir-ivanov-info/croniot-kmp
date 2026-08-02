package com.server.croniot.data.db.daos

import com.server.croniot.data.db.entities.DeviceEntity
import com.server.croniot.testsupport.PostgresTestcontainer
import croniot.models.Account
import croniot.models.ParameterTask
import croniot.models.TaskType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ParameterTaskDaoJooqImplIntegrationTest {

    private val accountDao = AccountJooqDaoImpl(PostgresTestcontainer.dsl)
    private val deviceDao = DeviceJooqDaoImpl(PostgresTestcontainer.dsl)
    private val taskTypeDao = TaskTypeDaoJooqImpl(PostgresTestcontainer.dsl)
    private val dao = ParameterTaskDaoJooqImpl(PostgresTestcontainer.dsl)

    @BeforeEach
    fun setUp() {
        PostgresTestcontainer.reset()
    }

    @Test
    fun `WHEN uid and taskTypeId match THEN getByUid returns the parameter with its constraints`() {
        val taskTypeId = insertTaskType(
            taskTypeUid = 1L,
            parameters = listOf(
                ParameterTask(
                    uid = 10L,
                    name = "duration",
                    type = "int",
                    unit = "s",
                    description = "duration param",
                    constraints = mapOf("min" to "1", "max" to "60"),
                )
            ),
        )

        val result = dao.getByUid(parameterTaskUid = 10L, taskTypeId = taskTypeId)

        assertNotNull(result)
        assertEquals(10L, result!!.uid)
        assertEquals("duration", result.name)
        assertEquals("int", result.type)
        assertEquals("s", result.unit)
        assertEquals("duration param", result.description)
        assertEquals(mapOf("min" to "1", "max" to "60"), result.constraints)
    }

    @Test
    fun `WHEN the parameter has no constraints THEN getByUid returns it with an empty constraints map`() {
        val taskTypeId = insertTaskType(
            taskTypeUid = 1L,
            parameters = listOf(
                ParameterTask(
                    uid = 20L,
                    name = "no-constraints",
                    type = "string",
                    unit = "",
                    description = "",
                    constraints = emptyMap(),
                )
            ),
        )

        val result = dao.getByUid(parameterTaskUid = 20L, taskTypeId = taskTypeId)

        assertNotNull(result)
        assertTrue(result!!.constraints.isEmpty())
    }

    @Test
    fun `WHEN the uid does not exist for the given taskTypeId THEN getByUid returns null`() {
        val taskTypeId = insertTaskType(taskTypeUid = 1L, parameters = emptyList())

        assertNull(dao.getByUid(parameterTaskUid = 999L, taskTypeId = taskTypeId))
    }

    @Test
    fun `WHEN the parameter exists but belongs to a different taskType THEN getByUid returns null`() {
        val taskTypeAId = insertTaskType(
            taskTypeUid = 1L,
            email = "a@example.com",
            deviceUuid = "dev-a",
            parameters = listOf(
                ParameterTask(uid = 30L, name = "p", type = "int", unit = "", description = "", constraints = emptyMap())
            ),
        )
        val taskTypeBId = insertTaskType(
            taskTypeUid = 2L,
            email = "b@example.com",
            deviceUuid = "dev-b",
            parameters = emptyList(),
        )

        assertNotNull(dao.getByUid(parameterTaskUid = 30L, taskTypeId = taskTypeAId))
        assertNull(dao.getByUid(parameterTaskUid = 30L, taskTypeId = taskTypeBId))
    }

    @Test
    fun `WHEN taskTypeId is 0 THEN getByUid throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            dao.getByUid(parameterTaskUid = 1L, taskTypeId = 0L)
        }
    }

    private fun insertTaskType(
        taskTypeUid: Long,
        parameters: List<ParameterTask>,
        deviceUuid: String = "device-uuid",
        email: String = "user-$deviceUuid@example.com",
    ): Long {
        val accountId = accountDao.insert(
            Account(uuid = "uuid-$email", nickname = "nick", email = email, devices = mutableListOf()),
            password = "pwd",
        )
        val deviceId = deviceDao.insert(
            DeviceEntity(uuid = deviceUuid, name = "name", description = "desc", iot = false, accountId = accountId)
        )
        return taskTypeDao.upsert(
            TaskType(uid = taskTypeUid, name = "tt-$taskTypeUid", description = "", parameters = parameters),
            deviceId = deviceId,
        )
    }
}
