package com.server.croniot.services

import com.server.croniot.application.JwtConfig
import com.server.croniot.data.db.daos.RefreshTokenDao
import com.server.croniot.data.db.daos.RefreshTokenDao.RefreshTokenRecord
import com.server.croniot.data.repositories.AccountRepository
import com.server.croniot.testsupport.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class RefreshTokenServiceTest {

    private val now: Instant = Instant.parse("2026-04-19T10:00:00Z")

    private val refreshTokenDao: RefreshTokenDao = mockk(relaxUnitFun = true)
    private val accountRepository: AccountRepository = mockk()
    private val secrets = Fixtures.secrets(jwtRefreshTokenTtlDays = 30L)
    private val jwtConfig = JwtConfig(secrets)

    private val service = RefreshTokenService(
        refreshTokenDao = refreshTokenDao,
        accountRepository = accountRepository,
        jwtConfig = jwtConfig,
        secrets = secrets,
    )

    @Test
    fun `issueForAccount persists a hashed token and returns the plaintext`() {
        val hashSlot = slot<String>()
        every {
            refreshTokenDao.create(
                accountId = 10L,
                tokenHash = capture(hashSlot),
                deviceUuid = "device-uuid",
                issuedAt = any(),
                expiresAt = any(),
            )
        } returns 1L

        val plaintext = service.issueForAccount(accountId = 10L, deviceUuid = "device-uuid")

        assertTrue(plaintext.isNotBlank())
        assertNotEquals(plaintext, hashSlot.captured)
        verify(exactly = 1) {
            refreshTokenDao.create(
                accountId = 10L,
                tokenHash = hashSlot.captured,
                deviceUuid = "device-uuid",
                issuedAt = any(),
                expiresAt = any(),
            )
        }
    }

    @Test
    fun `issueForAccount produces distinct plaintexts and distinct hashes between calls`() {
        val hashes = mutableListOf<String>()
        every {
            refreshTokenDao.create(any(), capture(hashes), any(), any(), any())
        } returns 1L

        val t1 = service.issueForAccount(accountId = 1L, deviceUuid = null)
        val t2 = service.issueForAccount(accountId = 1L, deviceUuid = null)

        assertNotEquals(t1, t2)
        assertNotEquals(hashes[0], hashes[1])
    }

    @Test
    fun `rotate returns null when token is unknown`() {
        every { refreshTokenDao.findByHash(any()) } returns null

        assertNull(service.rotate("unknown-plaintext"))
    }

    @Test
    fun `rotate returns null when token is revoked`() {
        every { refreshTokenDao.findByHash(any()) } returns record(
            expiresAt = now.plus(Duration.ofDays(10)),
            revokedAt = now.minus(Duration.ofMinutes(1)),
        )

        assertNull(service.rotate("any-plaintext"))
    }

    @Test
    fun `rotate returns null when token is expired`() {
        every { refreshTokenDao.findByHash(any()) } returns record(
            expiresAt = Instant.EPOCH,
            revokedAt = null,
        )

        assertNull(service.rotate("any-plaintext"))
    }

    @Test
    fun `rotate returns null when account email cannot be resolved`() {
        every { refreshTokenDao.findByHash(any()) } returns record(
            expiresAt = Instant.now().plus(Duration.ofDays(5)),
            revokedAt = null,
        )
        every { accountRepository.getEmailById(any()) } returns null

        assertNull(service.rotate("any-plaintext"))

        verify { refreshTokenDao.revokeById(any(), any()) }
    }

    @Test
    fun `rotate happy path revokes the old record, issues new access and new refresh`() {
        every { refreshTokenDao.findByHash(any()) } returns record(
            expiresAt = Instant.now().plus(Duration.ofDays(5)),
            revokedAt = null,
        )
        every { accountRepository.getEmailById(99L) } returns "user@example.com"
        every {
            refreshTokenDao.create(accountId = 99L, tokenHash = any(), deviceUuid = "dev-1", issuedAt = any(), expiresAt = any())
        } returns 2L

        val result = service.rotate("any-plaintext")

        assertNotNull(result)
        assertTrue(result!!.accessToken.isNotBlank())
        assertTrue(result.refreshToken.isNotBlank())
        assertTrue(result.accessTokenExpiresAtEpochSeconds > 0)
        verify(exactly = 1) { refreshTokenDao.revokeById(1L, any()) }
        verify(exactly = 1) {
            refreshTokenDao.create(accountId = 99L, tokenHash = any(), deviceUuid = "dev-1", issuedAt = any(), expiresAt = any())
        }
    }

    @Test
    fun `revoke marks the record when found and active`() {
        every { refreshTokenDao.findByHash(any()) } returns record(
            expiresAt = Instant.now().plus(Duration.ofDays(5)),
            revokedAt = null,
        )

        service.revoke("plaintext")

        verify(exactly = 1) { refreshTokenDao.revokeById(1L, any()) }
    }

    @Test
    fun `revoke is a no-op when token is already revoked`() {
        every { refreshTokenDao.findByHash(any()) } returns record(
            expiresAt = Instant.now().plus(Duration.ofDays(5)),
            revokedAt = Instant.now().minus(Duration.ofMinutes(1)),
        )

        service.revoke("plaintext")

        verify(exactly = 0) { refreshTokenDao.revokeById(any(), any()) }
    }

    @Test
    fun `revoke is a no-op when token does not exist`() {
        every { refreshTokenDao.findByHash(any()) } returns null

        service.revoke("plaintext")

        verify(exactly = 0) { refreshTokenDao.revokeById(any(), any()) }
    }

    private fun record(
        id: Long = 1L,
        accountId: Long = 99L,
        deviceUuid: String? = "dev-1",
        expiresAt: Instant,
        revokedAt: Instant?,
    ) = RefreshTokenRecord(
        id = id,
        accountId = accountId,
        tokenHash = "hash",
        deviceUuid = deviceUuid,
        issuedAt = Instant.EPOCH,
        expiresAt = expiresAt,
        revokedAt = revokedAt,
    )
}
