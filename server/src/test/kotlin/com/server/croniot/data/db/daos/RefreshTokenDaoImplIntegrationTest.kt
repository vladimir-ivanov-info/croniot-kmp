package com.server.croniot.data.db.daos

import com.server.croniot.testsupport.PostgresTestcontainer
import croniot.models.Account
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.jooq.exception.DataAccessException
import java.time.Instant
import java.time.temporal.ChronoUnit

class RefreshTokenDaoImplIntegrationTest {

    private val accountDao = AccountJooqDaoImpl(PostgresTestcontainer.dsl)
    private val dao = RefreshTokenDaoImpl(PostgresTestcontainer.dsl)

    private val now: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
    private val tomorrow: Instant = now.plus(1, ChronoUnit.DAYS)

    @BeforeEach
    fun setUp() {
        PostgresTestcontainer.reset()
    }

    @Test
    fun `create persists and returns a positive id`() {
        val accountId = insertAccount()

        val id = dao.create(
            accountId = accountId,
            tokenHash = "hash-1",
            deviceUuid = "device-1",
            issuedAt = now,
            expiresAt = tomorrow,
        )

        assertTrue(id > 0)
    }

    @Test
    fun `findByHash returns the record when present and null when absent`() {
        val accountId = insertAccount()
        val id = dao.create(accountId, "hash-2", "device-2", now, tomorrow)

        val found = dao.findByHash("hash-2")
        assertNotNull(found)
        assertEquals(id, found!!.id)
        assertEquals(accountId, found.accountId)
        assertEquals("hash-2", found.tokenHash)
        assertEquals("device-2", found.deviceUuid)
        assertEquals(now, found.issuedAt)
        assertEquals(tomorrow, found.expiresAt)
        assertNull(found.revokedAt)

        assertNull(dao.findByHash("missing-hash"))
    }

    @Test
    fun `create enforces the token_hash unique constraint`() {
        val accountId = insertAccount()
        dao.create(accountId, "dup", null, now, tomorrow)

        assertThrows<DataAccessException> {
            dao.create(accountId, "dup", null, now, tomorrow)
        }
    }

    @Test
    fun `revokeById stamps revoked_at and leaves other rows untouched`() {
        val accountId = insertAccount()
        val targetId = dao.create(accountId, "target", null, now, tomorrow)
        dao.create(accountId, "untouched", null, now, tomorrow)

        val revokedAt = now.plusSeconds(60)
        dao.revokeById(targetId, revokedAt)

        val target = dao.findByHash("target")
        assertNotNull(target!!.revokedAt)
        assertEquals(revokedAt, target.revokedAt)

        val untouched = dao.findByHash("untouched")
        assertNull(untouched!!.revokedAt)
    }

    @Test
    fun `revokeAllForAccount only affects rows of that account that are still active`() {
        val accountA = insertAccount(email = "a@example.com")
        val accountB = insertAccount(email = "b@example.com")
        val tokenA1 = dao.create(accountA, "a1", null, now, tomorrow)
        val tokenA2 = dao.create(accountA, "a2", null, now, tomorrow)
        dao.create(accountB, "b1", null, now, tomorrow)

        // Pre-revoke a1 with an explicit timestamp — revokeAllForAccount must not overwrite it.
        val preexistingRevokedAt = now.plusSeconds(10)
        dao.revokeById(tokenA1, preexistingRevokedAt)

        val bulkRevokedAt = now.plusSeconds(120)
        dao.revokeAllForAccount(accountA, bulkRevokedAt)

        val a1 = dao.findByHash("a1")!!
        val a2 = dao.findByHash("a2")!!
        val b1 = dao.findByHash("b1")!!

        assertEquals(preexistingRevokedAt, a1.revokedAt, "already-revoked row must not be overwritten")
        assertEquals(bulkRevokedAt, a2.revokedAt)
        assertNull(b1.revokedAt, "rows of other accounts must remain untouched")
    }

    @Test
    fun `create allows null deviceUuid`() {
        val accountId = insertAccount()

        dao.create(accountId, "no-device", deviceUuid = null, issuedAt = now, expiresAt = tomorrow)

        val record = dao.findByHash("no-device")!!
        assertNull(record.deviceUuid)
    }

    private fun insertAccount(email: String = "user@example.com"): Long {
        return accountDao.insert(
            Account(uuid = "uuid-$email", nickname = "nick", email = email, devices = mutableListOf()),
            password = "pwd",
        )
    }
}
