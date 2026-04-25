package com.server.croniot.data.db.daos

import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import org.jooq.impl.SQLDataType
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.inject.Inject

class RefreshTokenDaoImpl @Inject constructor(
    private val dsl: DSLContext,
) : RefreshTokenDao {

    private val refreshTokenTable = table("refresh_token")
    private val fId: Field<Long> = field("id", SQLDataType.BIGINT)
    private val fAccount: Field<Long> = field("account", SQLDataType.BIGINT)
    private val fTokenHash: Field<String> = field("token_hash", SQLDataType.VARCHAR)
    private val fDeviceUuid: Field<String> = field("device_uuid", SQLDataType.VARCHAR)
    private val fIssuedAt: Field<OffsetDateTime> = field("issued_at", SQLDataType.TIMESTAMPWITHTIMEZONE)
    private val fExpiresAt: Field<OffsetDateTime> = field("expires_at", SQLDataType.TIMESTAMPWITHTIMEZONE)
    private val fRevokedAt: Field<OffsetDateTime> = field("revoked_at", SQLDataType.TIMESTAMPWITHTIMEZONE)

    override fun create(
        accountId: Long,
        tokenHash: String,
        deviceUuid: String?,
        issuedAt: Instant,
        expiresAt: Instant,
    ): Long {
        return dsl.insertInto(refreshTokenTable)
            .set(fAccount, accountId)
            .set(fTokenHash, tokenHash)
            .set(fDeviceUuid, deviceUuid)
            .set(fIssuedAt, issuedAt.atOffset(ZoneOffset.UTC))
            .set(fExpiresAt, expiresAt.atOffset(ZoneOffset.UTC))
            .returning(fId)
            .fetchOne()
            ?.get(fId)
            ?: error("Failed to insert refresh_token")
    }

    override fun findByHash(tokenHash: String): RefreshTokenDao.RefreshTokenRecord? {
        val rec = dsl
            .select(fId, fAccount, fTokenHash, fDeviceUuid, fIssuedAt, fExpiresAt, fRevokedAt)
            .from(refreshTokenTable)
            .where(fTokenHash.eq(tokenHash))
            .fetchOne()
            ?: return null

        return RefreshTokenDao.RefreshTokenRecord(
            id = rec.get(fId),
            accountId = rec.get(fAccount),
            tokenHash = rec.get(fTokenHash),
            deviceUuid = rec.get(fDeviceUuid),
            issuedAt = rec.get(fIssuedAt).toInstant(),
            expiresAt = rec.get(fExpiresAt).toInstant(),
            revokedAt = rec.get(fRevokedAt)?.toInstant(),
        )
    }

    override fun revokeById(id: Long, revokedAt: Instant) {
        dsl.update(refreshTokenTable)
            .set(fRevokedAt, revokedAt.atOffset(ZoneOffset.UTC))
            .where(fId.eq(id))
            .execute()
    }

    override fun revokeAllForAccount(accountId: Long, revokedAt: Instant) {
        dsl.update(refreshTokenTable)
            .set(fRevokedAt, revokedAt.atOffset(ZoneOffset.UTC))
            .where(fAccount.eq(accountId))
            .and(fRevokedAt.isNull)
            .execute()
    }
}