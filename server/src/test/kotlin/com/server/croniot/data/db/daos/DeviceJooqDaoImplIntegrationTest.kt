package com.server.croniot.data.db.daos

import com.server.croniot.data.db.entities.DeviceEntity
import com.server.croniot.testsupport.PostgresTestcontainer
import croniot.models.Account
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.jooq.exception.DataAccessException

class DeviceJooqDaoImplIntegrationTest {

    private val accountDao = AccountJooqDaoImpl(PostgresTestcontainer.dsl)
    private val dao = DeviceJooqDaoImpl(PostgresTestcontainer.dsl)

    @BeforeEach
    fun setUp() {
        PostgresTestcontainer.reset()
    }

    @Test
    fun `WHEN insert is called THEN it persists the device and returns a positive id`() {
        val accountId = insertAccount()

        val id = dao.insert(device(uuid = "dev-1", accountId = accountId))

        assertTrue(id > 0)
    }

    @Test
    fun `WHEN accountId is zero THEN insert throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            dao.insert(device(uuid = "dev-orphan", accountId = 0L))
        }
    }

    @Test
    fun `WHEN uuid already exists THEN insert throws DataAccessException`() {
        val accountId = insertAccount()
        dao.insert(device(uuid = "dup", accountId = accountId))

        assertThrows<DataAccessException> {
            dao.insert(device(uuid = "dup", accountId = accountId))
        }
    }

    @Test
    fun `WHEN uuid is new THEN upsert inserts, otherwise it updates the existing row`() {
        val accountId = insertAccount()

        val firstId = dao.upsert(
            device(uuid = "shared-uuid", name = "initial", description = "v1", iot = false, accountId = accountId),
        )
        val secondId = dao.upsert(
            device(uuid = "shared-uuid", name = "updated", description = "v2", iot = true, accountId = accountId),
        )

        assertEquals(firstId, secondId, "upsert must return the same id on conflict")

        val stored = dao.getByUuid("shared-uuid")
        assertNotNull(stored)
        assertEquals("updated", stored!!.name)
        assertEquals("v2", stored.description)
        assertTrue(stored.iot)
    }

    @Test
    fun `WHEN device is present THEN getByUuid returns it, otherwise null`() {
        val accountId = insertAccount()
        dao.insert(device(uuid = "known", name = "Thermostat", accountId = accountId))

        val found = dao.getByUuid("known")
        assertNotNull(found)
        assertEquals("known", found!!.uuid)
        assertEquals("Thermostat", found.name)

        assertNull(dao.getByUuid("unknown"))
    }

    @Test
    fun `WHEN uuid is known THEN getLazy returns a minimal projection, otherwise null`() {
        val accountId = insertAccount()
        dao.insert(device(uuid = "lazy-known", name = "ignored-by-lazy", accountId = accountId))

        val lazy = dao.getLazy("lazy-known")
        assertNotNull(lazy)
        assertEquals("lazy-known", lazy!!.uuid)
        // Lazy projection returns default/empty values for name/description/iot.
        assertEquals("", lazy.name)
        assertEquals("", lazy.description)
        assertFalse(lazy.iot)

        assertNull(dao.getLazy("missing"))
    }

    @Test
    fun `WHEN uuid exists THEN getDeviceId returns the id, otherwise null`() {
        val accountId = insertAccount()
        val id = dao.insert(device(uuid = "has-id", accountId = accountId))

        assertEquals(id, dao.getDeviceId("has-id"))
        assertNull(dao.getDeviceId("no-such-uuid"))
    }

    @Test
    fun `WHEN uuid is present THEN isDeviceExists returns true, otherwise false`() {
        val accountId = insertAccount()
        dao.insert(device(uuid = "present", accountId = accountId))

        assertTrue(dao.isDeviceExists("present"))
        assertFalse(dao.isDeviceExists("absent"))
    }

    @Test
    fun `WHEN account has android and non-android devices THEN getDevices returns only the non-android rows of that account`() {
        val accountA = insertAccount(email = "a@example.com")
        val accountB = insertAccount(email = "b@example.com")

        dao.insert(device(uuid = "real-1", name = "physical", accountId = accountA))
        dao.insert(device(uuid = "real-2", name = "gateway", accountId = accountA))
        // android-prefixed devices must be filtered out.
        dao.insert(device(uuid = "android-phone", name = "mobile", accountId = accountA))
        // Other account's rows must not leak in.
        dao.insert(device(uuid = "other", name = "other-acc", accountId = accountB))

        val devices = dao.getDevices(accountA)

        assertEquals(2, devices.size)
        val uuids = devices.map { it.uuid }.toSet()
        assertTrue(uuids.containsAll(setOf("real-1", "real-2")))
        assertFalse(uuids.contains("android-phone"))
        assertFalse(uuids.contains("other"))
    }

    @Test
    fun `WHEN devices belong to different accounts and uuid prefixes THEN getAll returns every row regardless`() {
        val accountA = insertAccount(email = "all-a@example.com")
        val accountB = insertAccount(email = "all-b@example.com")
        dao.insert(device(uuid = "all-1", accountId = accountA))
        dao.insert(device(uuid = "android-all", accountId = accountA))
        dao.insert(device(uuid = "all-2", accountId = accountB))

        val all = dao.getAll()

        assertEquals(3, all.size)
        val uuids = all.map { it.uuid }.toSet()
        assertEquals(setOf("all-1", "android-all", "all-2"), uuids)
    }

    private fun insertAccount(email: String = "user@example.com"): Long {
        return accountDao.insert(
            Account(uuid = "uuid-$email", nickname = "nick", email = email, devices = mutableListOf()),
            password = "pwd",
        )
    }

    private fun device(
        uuid: String,
        name: String = "name",
        description: String = "desc",
        iot: Boolean = false,
        accountId: Long,
    ): DeviceEntity = DeviceEntity(
        uuid = uuid,
        name = name,
        description = description,
        iot = iot,
        accountId = accountId,
    )
}
