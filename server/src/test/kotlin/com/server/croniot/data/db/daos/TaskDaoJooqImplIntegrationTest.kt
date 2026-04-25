package com.server.croniot.data.db.daos

import com.server.croniot.data.db.entities.DeviceEntity
import com.server.croniot.testsupport.PostgresTestcontainer
import croniot.models.Account
import croniot.models.Task
import croniot.models.TaskStateInfo
import croniot.models.TaskType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class TaskDaoJooqImplIntegrationTest {

    private val accountDao = AccountJooqDaoImpl(PostgresTestcontainer.dsl)
    private val deviceDao = DeviceJooqDaoImpl(PostgresTestcontainer.dsl)
    private val taskTypeDao = TaskTypeDaoJooqImpl(PostgresTestcontainer.dsl)
    private val stateInfoDao = TaskStateInfoDaoJooqImpl(PostgresTestcontainer.dsl)
    private val dao = TaskDaoJooqImpl(PostgresTestcontainer.dsl)

    @BeforeEach
    fun setUp() {
        PostgresTestcontainer.reset()
    }

    @Test
    fun `create inserts a Task row and returns a Task with the generated uid`() {
        val (_, taskTypeId) = insertTaskType(taskTypeUid = 10L)

        val task = dao.create(taskTypeId = taskTypeId, taskTypeUid = 10L)

        assertNotNull(task)
        assertEquals(10L, task!!.taskTypeUid)
        assertTrue(task.uid >= 0L, "uid is generated as a non-negative random Long")
    }

    @Test
    fun `get returns the matching task by (deviceUuid, taskTypeUid, taskUid) and null otherwise`() {
        val (deviceUuid, taskTypeId) = insertTaskType(deviceUuid = "dev-1", taskTypeUid = 42L)
        val created = dao.create(taskTypeId, 42L)!!

        val found = dao.get(deviceUuid, 42L, created.uid)
        assertNotNull(found)
        assertEquals(created.uid, found!!.uid)
        assertEquals(42L, found.taskTypeUid)

        assertNull(dao.get(deviceUuid, 42L, 999L))
        assertNull(dao.get("wrong-device", 42L, created.uid))
        assertNull(dao.get(deviceUuid, 7L, created.uid))
    }

    @Test
    fun `getAll returns tasks for a device with the most recent state info attached`() {
        val (deviceUuid, taskTypeId) = insertTaskType(deviceUuid = "dev-gall", taskTypeUid = 1L)
        val created = dao.create(taskTypeId, 1L)!!
        val taskId = taskIdForUid(created.uid)

        val early = ZonedDateTime.of(2026, 4, 20, 9, 0, 0, 0, ZoneOffset.UTC)
        val late = early.plusHours(1)
        stateInfoDao.insert(stateInfo(created.uid, early, "STARTED", 0.0), taskId)
        stateInfoDao.insert(stateInfo(created.uid, late, "RUNNING", 0.5), taskId)

        val all = dao.getAll(deviceUuid)
        val retrieved = all.single()
        assertEquals(created.uid, retrieved.uid)
        assertEquals("RUNNING", retrieved.mostRecentStateInfo?.state)
        assertEquals(0.5, retrieved.mostRecentStateInfo?.progress)
    }

    @Test
    fun `getAll returns empty list when the device has no tasks`() {
        val (deviceUuid, _) = insertTaskType(deviceUuid = "dev-empty", taskTypeUid = 1L)
        assertTrue(dao.getAll(deviceUuid).isEmpty())
        assertTrue(dao.getAll("missing-device").isEmpty())
    }

    @Test
    fun `getAllStateInfoHistory orders by dateTime desc, id desc and respects limit`() {
        val (deviceUuid, taskTypeId) = insertTaskType(deviceUuid = "dev-hist", taskTypeUid = 1L)
        val task = dao.create(taskTypeId, 1L)!!
        val taskId = taskIdForUid(task.uid)

        val t0 = ZonedDateTime.of(2026, 4, 20, 8, 0, 0, 0, ZoneOffset.UTC)
        val t1 = t0.plusHours(1)
        val t2 = t0.plusHours(2)
        stateInfoDao.insert(stateInfo(task.uid, t0, "A", 0.0), taskId)
        stateInfoDao.insert(stateInfo(task.uid, t1, "B", 0.1), taskId)
        stateInfoDao.insert(stateInfo(task.uid, t2, "C", 0.9), taskId)

        val all = dao.getAllStateInfoHistory(deviceUuid, limit = 50, before = null, beforeId = null)
        assertEquals(listOf("C", "B", "A"), all.map { it.state })

        val page = dao.getAllStateInfoHistory(deviceUuid, limit = 2, before = null, beforeId = null)
        assertEquals(2, page.size)
        assertEquals(listOf("C", "B"), page.map { it.state })
    }

    @Test
    fun `getAllStateInfoHistory uses (before, beforeId) as a cursor with id tiebreaker on ties`() {
        val (deviceUuid, taskTypeId) = insertTaskType(deviceUuid = "dev-cursor", taskTypeUid = 1L)
        val task = dao.create(taskTypeId, 1L)!!
        val taskId = taskIdForUid(task.uid)

        val t0 = ZonedDateTime.of(2026, 4, 20, 12, 0, 0, 0, ZoneOffset.UTC)
        stateInfoDao.insert(stateInfo(task.uid, t0.minusHours(1), "OLDEST", 0.0), taskId)
        stateInfoDao.insert(stateInfo(task.uid, t0, "MIDDLE", 0.5), taskId)
        stateInfoDao.insert(stateInfo(task.uid, t0.plusHours(1), "NEWEST", 1.0), taskId)

        // With beforeId = null, the DAO falls back to Long.MAX_VALUE, so rows with
        // DATE_TIME == before are INCLUDED (id < MAX_VALUE always holds).
        val beforeMiddle = t0.toOffsetDateTime()
        val withTimestampEqual = dao.getAllStateInfoHistory(
            deviceUuid, 50, before = beforeMiddle, beforeId = null,
        )
        assertEquals(listOf("MIDDLE", "OLDEST"), withTimestampEqual.map { it.state })

        // With beforeId = MIDDLE's id, the tiebreaker excludes MIDDLE itself.
        val middleEntry = withTimestampEqual.single { it.state == "MIDDLE" }
        val strictlyBefore = dao.getAllStateInfoHistory(
            deviceUuid,
            limit = 50,
            before = beforeMiddle,
            beforeId = middleEntry.stateInfoId,
        )
        assertEquals(listOf("OLDEST"), strictlyBefore.map { it.state })
    }

    @Test
    fun `getAllStateInfoHistory filters by taskTypeUid`() {
        val accountId = insertAccount()
        val deviceId = deviceDao.insert(device(accountId, "dev-filter"))
        val ttA = taskTypeDao.upsert(TaskType(uid = 1L, name = "A", description = ""), deviceId)
        val ttB = taskTypeDao.upsert(TaskType(uid = 2L, name = "B", description = ""), deviceId)
        val taskA = dao.create(ttA, 1L)!!
        val taskB = dao.create(ttB, 2L)!!

        val t = ZonedDateTime.of(2026, 4, 20, 10, 0, 0, 0, ZoneOffset.UTC)
        stateInfoDao.insert(stateInfo(taskA.uid, t, "for-A", 0.0), taskIdForUid(taskA.uid))
        stateInfoDao.insert(stateInfo(taskB.uid, t, "for-B", 0.0), taskIdForUid(taskB.uid))

        val onlyA = dao.getAllStateInfoHistory("dev-filter", 50, null, null, taskTypeUid = 1L)
        assertEquals(listOf("for-A"), onlyA.map { it.state })
    }

    @Test
    fun `getAllStateInfoHistoryCount mirrors the filters of getAllStateInfoHistory`() {
        val (deviceUuid, taskTypeId) = insertTaskType(deviceUuid = "dev-count", taskTypeUid = 1L)
        val task = dao.create(taskTypeId, 1L)!!
        val taskId = taskIdForUid(task.uid)
        val t = ZonedDateTime.of(2026, 4, 20, 10, 0, 0, 0, ZoneOffset.UTC)
        stateInfoDao.insert(stateInfo(task.uid, t.minusHours(2), "A", 0.0), taskId)
        stateInfoDao.insert(stateInfo(task.uid, t.minusHours(1), "B", 0.0), taskId)
        stateInfoDao.insert(stateInfo(task.uid, t, "C", 0.0), taskId)

        assertEquals(3, dao.getAllStateInfoHistoryCount(deviceUuid, null, null))
        // Strictly before t: only A and B, since beforeId < C's id excludes C on the tiebreaker.
        val cEntry = dao
            .getAllStateInfoHistory(deviceUuid, 50, null, null)
            .single { it.state == "C" }
        assertEquals(
            2,
            dao.getAllStateInfoHistoryCount(
                deviceUuid,
                before = t.toOffsetDateTime(),
                beforeId = cEntry.stateInfoId,
            ),
        )
        assertEquals(0, dao.getAllStateInfoHistoryCount(deviceUuid, null, null, taskTypeUid = 999L))
    }

    @Test
    fun `TaskStateInfoDao insert rejects non-existent taskId`() {
        val stateInfo = stateInfo(
            taskUid = 1L,
            dateTime = ZonedDateTime.now(ZoneId.systemDefault()),
            state = "S",
            progress = 0.0,
        )
        try {
            stateInfoDao.insert(stateInfo, taskId = 99_999L)
            error("expected IllegalArgumentException")
        } catch (ex: Exception) {
            val cause = (ex as? IllegalArgumentException) ?: (ex.cause as? IllegalArgumentException)
            assertNotNull(cause, "expected IllegalArgumentException (possibly wrapped), got $ex")
            assertTrue(cause!!.message?.contains("99999") == true)
        }
    }

    private fun insertAccount(email: String = "user@example.com"): Long = accountDao.insert(
        Account(uuid = "uuid-$email", nickname = "nick", email = email, devices = mutableListOf()),
        password = "pwd",
    )

    private fun device(accountId: Long, uuid: String = "device-uuid") = DeviceEntity(
        uuid = uuid,
        name = "name",
        description = "desc",
        iot = false,
        accountId = accountId,
    )

    private fun insertTaskType(
        deviceUuid: String = "device-uuid",
        taskTypeUid: Long = 1L,
        email: String = "user-$deviceUuid@example.com",
    ): Pair<String, Long> {
        val accountId = insertAccount(email)
        val deviceId = deviceDao.insert(device(accountId, deviceUuid))
        val taskTypeId = taskTypeDao.upsert(
            TaskType(uid = taskTypeUid, name = "tt-$taskTypeUid", description = ""),
            deviceId = deviceId,
        )
        return deviceUuid to taskTypeId
    }

    private fun taskIdForUid(taskUid: Long): Long =
        PostgresTestcontainer.dsl
            .fetchOne("SELECT id FROM task WHERE uid = ?", taskUid)!!
            .getValue("id") as Long

    private fun stateInfo(
        taskUid: Long,
        dateTime: ZonedDateTime,
        state: String,
        progress: Double,
        errorMessage: String = "",
    ): TaskStateInfo = TaskStateInfo(
        taskUid = taskUid,
        dateTime = dateTime,
        state = state,
        progress = progress,
        errorMessage = errorMessage,
    )

}
