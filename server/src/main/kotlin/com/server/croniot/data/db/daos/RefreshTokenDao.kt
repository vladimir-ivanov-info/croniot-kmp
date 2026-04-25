package com.server.croniot.data.db.daos

import java.time.Instant

interface RefreshTokenDao {

    data class RefreshTokenRecord(
        val id: Long,
        val accountId: Long,
        val tokenHash: String,
        val deviceUuid: String?,
        val issuedAt: Instant,
        val expiresAt: Instant,
        val revokedAt: Instant?,
    )

    fun create(
        accountId: Long,
        tokenHash: String,
        deviceUuid: String?,
        issuedAt: Instant,
        expiresAt: Instant,
    ): Long

    fun findByHash(tokenHash: String): RefreshTokenRecord?

    fun revokeById(id: Long, revokedAt: Instant)

    fun revokeAllForAccount(accountId: Long, revokedAt: Instant)
}