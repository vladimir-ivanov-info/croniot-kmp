package com.server.croniot.data.db.daos

import com.server.croniot.testsupport.PostgresTestcontainer
import croniot.models.Account
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

    private fun account(uuid: String, email: String): Account = Account(
        uuid = uuid,
        nickname = "nick",
        email = email,
        devices = mutableListOf(),
    )
}
