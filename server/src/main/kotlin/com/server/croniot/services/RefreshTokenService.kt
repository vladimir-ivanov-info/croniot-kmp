package com.server.croniot.services

import com.server.croniot.application.JwtConfig
import com.server.croniot.config.Secrets
import com.server.croniot.data.db.daos.RefreshTokenDao
import com.server.croniot.data.repositories.AccountRepository
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefreshTokenService @Inject constructor(
    private val refreshTokenDao: RefreshTokenDao,
    private val accountRepository: AccountRepository,
    private val jwtConfig: JwtConfig,
    private val secrets: Secrets,
) {

    private val random = SecureRandom()
    private val clock: Clock = Clock.systemUTC()

    data class RotationResult(
        val accessToken: String,
        val accessTokenExpiresAtEpochSeconds: Long,
        val refreshToken: String,
    )

    fun issueForAccount(accountId: Long, deviceUuid: String?): String {
        val (plaintext, hash) = generateTokenAndHash()
        val now = Instant.now(clock)
        val ttl = Duration.ofDays(secrets.jwtRefreshTokenTtlDays)
        refreshTokenDao.create(
            accountId = accountId,
            tokenHash = hash,
            deviceUuid = deviceUuid,
            issuedAt = now,
            expiresAt = now.plus(ttl),
        )
        return plaintext
    }

    fun rotate(oldPlaintext: String): RotationResult? {
        val hash = sha256(oldPlaintext)
        val record = refreshTokenDao.findByHash(hash) ?: return null
        val now = Instant.now(clock)
        if (record.revokedAt != null) return null
        if (record.expiresAt.isBefore(now)) return null

        refreshTokenDao.revokeById(record.id, now)
        val email = accountRepository.getEmailById(record.accountId) ?: return null
        val newAccess = jwtConfig.issueAccessToken(record.accountId, email, now)
        val newRefresh = issueForAccount(record.accountId, record.deviceUuid)
        return RotationResult(
            accessToken = newAccess.token,
            accessTokenExpiresAtEpochSeconds = newAccess.expiresAt.epochSecond,
            refreshToken = newRefresh,
        )
    }

    fun revoke(plaintext: String) {
        val hash = sha256(plaintext)
        val record = refreshTokenDao.findByHash(hash) ?: return
        if (record.revokedAt != null) return
        refreshTokenDao.revokeById(record.id, Instant.now(clock))
    }

    private fun generateTokenAndHash(): Pair<String, String> {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        val plaintext = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return plaintext to sha256(plaintext)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private companion object {
        const val TOKEN_BYTES = 32
    }
}