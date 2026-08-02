package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.VerifyPasswordResult
import com.server.croniot.data.db.entities.AccountEntity
import com.server.croniot.data.db.entities.DeviceEntity
import com.server.croniot.testsupport.fakes.FakeAccountDao
import com.server.croniot.testsupport.fakes.FakeDeviceDao
import com.server.croniot.testsupport.fakes.FakeSensorTypeDao
import com.server.croniot.testsupport.fakes.FakeTaskTypeDao
import croniot.models.SensorType
import croniot.models.TaskType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [AccountRepository]'s own orchestration logic, using fake DAOs (not mocks): this
 * class combines results from 4 DAOs, and a fake exercises that combination with realistic
 * write-then-read behavior instead of hand-stubbing every call — see docs on the fakes-vs-mocks
 * convention. The DAO layer itself is already covered against a real Postgres by
 * AccountJooqDaoImplIntegrationTest.
 */
class AccountRepositoryTest {

    private val accountDao = FakeAccountDao()
    private val deviceDao = FakeDeviceDao()
    private val sensorTypeDao = FakeSensorTypeDao()
    private val taskTypeDao = FakeTaskTypeDao()
    private val repository = AccountRepository(accountDao, deviceDao, sensorTypeDao, taskTypeDao)

    @Test
    fun `WHEN email is unknown THEN getAccount returns null`() {
        assertNull(repository.getAccount("missing@example.com"))
    }

    @Test
    fun `WHEN account has devices with sensor and task types THEN getAccount assembles them all`() {
        accountDao.seed(AccountEntity(id = 1L, uuid = "acc-uuid", nickname = "nick", email = "user@example.com", password = "hash"))
        val deviceId = deviceDao.seed(
            DeviceEntity(uuid = "device-uuid", name = "Device", description = "", iot = true, accountId = 1L),
        )
        val sensorType = SensorType(uid = 10L, name = "Temp", description = "", parameters = emptyList())
        val taskType = TaskType(uid = 20L, name = "Water", description = "")
        sensorTypeDao.seed(deviceId, sensorType)
        taskTypeDao.seed(deviceId, taskType)

        val account = repository.getAccount("user@example.com")

        assertEquals("acc-uuid", account?.uuid)
        assertEquals(1, account?.devices?.size)
        assertEquals("device-uuid", account?.devices?.first()?.uuid)
        assertEquals(listOf(sensorType), account?.devices?.first()?.sensorTypes)
        assertEquals(listOf(taskType), account?.devices?.first()?.taskTypes)
    }

    @Test
    fun `WHEN email is known THEN getAccountId returns the id, otherwise null`() {
        accountDao.seed(AccountEntity(id = 5L, uuid = "u", nickname = "n", email = "a@b.com", password = "h"))

        assertEquals(5L, repository.getAccountId("a@b.com"))
        assertNull(repository.getAccountId("missing@example.com"))
    }

    @Test
    fun `WHEN id is known THEN getEmailById returns the email, otherwise null`() {
        accountDao.seed(AccountEntity(id = 5L, uuid = "u", nickname = "n", email = "a@b.com", password = "h"))

        assertEquals("a@b.com", repository.getEmailById(5L))
        assertNull(repository.getEmailById(999L))
    }

    @Test
    fun `WHEN nobody has that email THEN isEmailAvailable is true`() {
        assertTrue(repository.isEmailAvailable("free@example.com"))
    }

    @Test
    fun `WHEN the email is taken THEN isEmailAvailable is false`() {
        accountDao.seed(AccountEntity(id = 1L, uuid = "u", nickname = "n", email = "taken@example.com", password = "h"))

        assertFalse(repository.isEmailAvailable("taken@example.com"))
    }

    @Test
    fun `WHEN createAccount is called THEN the account is persisted and becomes retrievable`() {
        val id = repository.createAccount("acc-uuid", "nick", "new@example.com", "secret")

        assertTrue(id > 0)
        assertTrue(repository.isAccountExists("new@example.com"))
    }

    @Test
    fun `WHEN the account was created THEN isAccountExists reflects that`() {
        assertFalse(repository.isAccountExists("nobody@example.com"))
        repository.createAccount("acc-uuid", "nick", "somebody@example.com", "secret")
        assertTrue(repository.isAccountExists("somebody@example.com"))
    }

    @Test
    fun `WHEN verifyPassword is called THEN it delegates to the dao result`() {
        accountDao.verifyPasswordResult = VerifyPasswordResult.Valid(rehashed = true)

        assertEquals(VerifyPasswordResult.Valid(rehashed = true), repository.verifyPassword("a@b.com", "secret"))
    }

    @Test
    fun `WHEN account is admin THEN isAdmin is true, otherwise false`() {
        accountDao.seed(AccountEntity(id = 1L, uuid = "u", nickname = "n", email = "admin@example.com", password = "h", isAdmin = true))
        accountDao.seed(AccountEntity(id = 2L, uuid = "u2", nickname = "n2", email = "user@example.com", password = "h", isAdmin = false))

        assertTrue(repository.isAdmin("admin@example.com"))
        assertFalse(repository.isAdmin("user@example.com"))
        assertFalse(repository.isAdmin("missing@example.com"))
    }

    @Test
    fun `WHEN getAccountEagerSkipTasks is called THEN it returns the account without tasks`() {
        accountDao.seed(AccountEntity(id = 1L, uuid = "u", nickname = "n", email = "a@b.com", password = "h"))

        assertEquals("u", repository.getAccountEagerSkipTasks("a@b.com")?.uuid)
    }
}
