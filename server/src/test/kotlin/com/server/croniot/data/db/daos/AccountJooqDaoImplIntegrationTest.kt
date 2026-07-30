package com.server.croniot.data.db.daos

import com.server.croniot.data.db.entities.DeviceEntity
import com.server.croniot.testsupport.PostgresTestcontainer
import croniot.models.Account
import croniot.models.ParameterSensor
import croniot.models.ParameterTask
import croniot.models.SensorType
import croniot.models.TaskType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mindrot.jbcrypt.BCrypt

class AccountJooqDaoImplIntegrationTest {

    private val dao = AccountJooqDaoImpl(PostgresTestcontainer.dsl)
    private val deviceDao = DeviceJooqDaoImpl(PostgresTestcontainer.dsl)
    private val sensorTypeDao = SensorTypeJooqDaoImpl(PostgresTestcontainer.dsl)
    private val taskTypeDao = TaskTypeDaoJooqImpl(PostgresTestcontainer.dsl)

    @BeforeEach
    fun setUp() {
        PostgresTestcontainer.reset()
    }

    @Test
    fun `insert persists account with a bcrypt hash (not the plaintext password)`() {
        val id = dao.insert(account("acc-1", "user@example.com"), password = "plaintext-pwd")

        assertTrue(id > 0)
        val storedPassword = PostgresTestcontainer.dsl
            .fetchOne("SELECT password FROM account WHERE id = ?", id)!!
            .getValue("password") as String
        assertTrue(storedPassword.startsWith("\$2a\$"), "expected bcrypt hash, got: $storedPassword")
        assertTrue(BCrypt.checkpw("plaintext-pwd", storedPassword))
    }

    @Test
    fun `get returns entity when email exists and null otherwise`() {
        dao.insert(account("acc-2", "present@example.com"), password = "pwd")

        val present = dao.get("present@example.com")
        assertNotNull(present)
        assertEquals("present@example.com", present!!.email)

        val absent = dao.get("missing@example.com")
        assertNull(absent)
    }

    @Test
    fun `verifyPassword returns Valid(rehashed=false) for bcrypt-stored password`() {
        dao.insert(account("acc-3", "user3@example.com"), password = "correct")

        val ok = dao.verifyPassword("user3@example.com", "correct")
        assertInstanceOf(VerifyPasswordResult.Valid::class.java, ok)
        assertEquals(false, (ok as VerifyPasswordResult.Valid).rehashed)
    }

    @Test
    fun `verifyPassword returns Invalid when password does not match`() {
        dao.insert(account("acc-4", "user4@example.com"), password = "correct")

        val result = dao.verifyPassword("user4@example.com", "wrong")
        assertEquals(VerifyPasswordResult.Invalid, result)
    }

    @Test
    fun `verifyPassword returns UserNotFound when email is unknown`() {
        val result = dao.verifyPassword("nobody@example.com", "anything")
        assertEquals(VerifyPasswordResult.UserNotFound, result)
    }

    @Test
    fun `verifyPassword re-hashes legacy plaintext password on first successful login`() {
        // Simulate legacy row inserted with plaintext (bypass DAO's bcrypt hashing).
        PostgresTestcontainer.dsl.execute(
            "INSERT INTO account (uuid, nickname, email, password) VALUES (?, ?, ?, ?)",
            "legacy-uuid",
            "legacy",
            "legacy@example.com",
            "legacy-plaintext",
        )

        val firstAttempt = dao.verifyPassword("legacy@example.com", "legacy-plaintext")
        assertInstanceOf(VerifyPasswordResult.Valid::class.java, firstAttempt)
        assertEquals(true, (firstAttempt as VerifyPasswordResult.Valid).rehashed)

        val stored = PostgresTestcontainer.dsl
            .fetchOne("SELECT password FROM account WHERE email = ?", "legacy@example.com")!!
            .getValue("password") as String
        assertTrue(stored.startsWith("\$2a\$"), "password should be re-hashed to bcrypt: $stored")

        val secondAttempt = dao.verifyPassword("legacy@example.com", "legacy-plaintext")
        assertInstanceOf(VerifyPasswordResult.Valid::class.java, secondAttempt)
        assertEquals(false, (secondAttempt as VerifyPasswordResult.Valid).rehashed)
    }

    @Test
    fun `isExistsAccountWithEmail returns true when account exists, false otherwise`() {
        dao.insert(account("acc-5", "present@example.com"), password = "pwd")

        assertTrue(dao.isExistsAccountWithEmail("present@example.com"))
        assertEquals(false, dao.isExistsAccountWithEmail("missing@example.com"))
    }

    @Test
    fun `getAccountId returns id when email exists and null otherwise`() {
        val createdId = dao.insert(account("acc-6", "id@example.com"), password = "pwd")

        assertEquals(createdId, dao.getAccountId("id@example.com"))
        assertNull(dao.getAccountId("unknown@example.com"))
    }

    @Test
    fun `getEmailById returns email when id exists and null otherwise`() {
        val createdId = dao.insert(account("acc-7", "email@example.com"), password = "pwd")

        assertEquals("email@example.com", dao.getEmailById(createdId))
        assertNull(dao.getEmailById(99_999L))
    }

    @Test
    fun `getAll returns every account with devices left empty`() {
        dao.insert(account("acc-a", "a@example.com"), password = "pwd")
        dao.insert(account("acc-b", "b@example.com"), password = "pwd")

        val all = dao.getAll()

        assertEquals(2, all.size)
        assertEquals(setOf("a@example.com", "b@example.com"), all.map { it.email }.toSet())
        assertTrue(all.all { it.devices.isEmpty() })
    }

    @Test
    fun `getAll returns empty list when there are no accounts`() {
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun `isAccountExists returns true when account exists, false otherwise`() {
        dao.insert(account("acc-exists", "exists@example.com"), password = "pwd")

        assertTrue(dao.isAccountExists("exists@example.com"))
        assertEquals(false, dao.isAccountExists("nope@example.com"))
    }

    @Test
    fun `getAccountEagerSkipTasks returns null when email does not exist`() {
        assertNull(dao.getAccountEagerSkipTasks("missing@example.com"))
    }

    @Test
    fun `getAccountEagerSkipTasks returns account with empty devices when account has no devices`() {
        dao.insert(account("acc-nodev", "nodev@example.com"), password = "pwd")

        val result = dao.getAccountEagerSkipTasks("nodev@example.com")

        assertNotNull(result)
        assertEquals("nodev@example.com", result!!.email)
        assertTrue(result.devices.isEmpty())
    }

    @Test
    fun `getAccountEagerSkipTasks returns device with empty sensorTypes and taskTypes when device has none`() {
        val accountId = dao.insert(account("acc-emptydevice", "emptydevice@example.com"), password = "pwd")
        deviceDao.insert(
            DeviceEntity(uuid = "dev-empty", name = "D", description = "", iot = false, accountId = accountId)
        )

        val result = dao.getAccountEagerSkipTasks("emptydevice@example.com")

        assertNotNull(result)
        val device = result!!.devices.single()
        assertEquals("dev-empty", device.uuid)
        assertTrue(device.sensorTypes.isEmpty())
        assertTrue(device.taskTypes.isEmpty())
    }

    @Test
    fun `getAccountEagerSkipTasks assembles the full device-sensorType-taskType graph with constraints`() {
        val accountId = dao.insert(account("acc-eager", "eager@example.com"), password = "pwd")
        val deviceId = deviceDao.insert(
            DeviceEntity(uuid = "dev-eager", name = "Device1", description = "desc", iot = true, accountId = accountId)
        )

        sensorTypeDao.upsert(
            SensorType(
                uid = 1L,
                name = "Temp",
                description = "temp sensor",
                parameters = listOf(
                    ParameterSensor(
                        uid = 1L,
                        name = "temperature",
                        type = "float",
                        unit = "C",
                        description = "temp param",
                        constraints = mapOf("min" to "0", "max" to "100"),
                    )
                ),
            ),
            deviceId = deviceId,
        )

        taskTypeDao.upsert(
            TaskType(
                uid = 1L,
                name = "Water",
                description = "water task",
                parameters = listOf(
                    ParameterTask(
                        uid = 1L,
                        name = "duration",
                        type = "int",
                        unit = "s",
                        description = "duration param",
                        constraints = mapOf("min" to "1"),
                    )
                ),
            ),
            deviceId = deviceId,
        )

        val result = dao.getAccountEagerSkipTasks("eager@example.com")

        assertNotNull(result)
        assertEquals("eager@example.com", result!!.email)
        val device = result.devices.single()
        assertEquals("dev-eager", device.uuid)
        assertEquals("Device1", device.name)
        assertTrue(device.iot)

        val sensorType = device.sensorTypes.single()
        assertEquals("Temp", sensorType.name)
        val sensorParam = sensorType.parameters.single()
        assertEquals("temperature", sensorParam.name)
        assertEquals(mapOf("min" to "0", "max" to "100"), sensorParam.constraints)

        val taskType = device.taskTypes.single()
        assertEquals("Water", taskType.name)
        val taskParam = taskType.parameters.single()
        assertEquals("duration", taskParam.name)
        assertEquals(mapOf("min" to "1"), taskParam.constraints)
    }

    private fun account(uuid: String, email: String): Account = Account(
        uuid = uuid,
        nickname = "nick",
        email = email,
        devices = mutableListOf(),
    )
}
