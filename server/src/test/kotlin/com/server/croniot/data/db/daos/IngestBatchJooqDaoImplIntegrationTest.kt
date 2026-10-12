package com.server.croniot.data.db.daos

import com.server.croniot.data.db.entities.DeviceEntity
import com.server.croniot.jooq.tables.IngestBatch.Companion.INGEST_BATCH
import com.server.croniot.testsupport.PostgresTestcontainer
import croniot.models.Account
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IngestBatchJooqDaoImplIntegrationTest {

    private val dsl = PostgresTestcontainer.dsl
    private val dao = IngestBatchJooqDaoImpl(dsl)

    @BeforeEach
    fun setUp() {
        PostgresTestcontainer.reset()
    }

    private fun insertDevice(): Long {
        val accountId = AccountJooqDaoImpl(dsl).insert(
            Account(uuid = "acc", nickname = "nick", email = "a@example.com", devices = mutableListOf()),
            password = "pwd",
        )
        return DeviceJooqDaoImpl(dsl).insert(
            DeviceEntity(uuid = "dev", name = "n", description = "d", iot = true, accountId = accountId),
        )
    }

    @Test
    fun `WHEN a batch is recorded THEN it is stored with every count and gets an id`() {
        val deviceId = insertDevice()
        val row = IngestBatchRow(
            deviceId = deviceId, stream = "data", bootId = 5, firstSeq = 10, lastSeq = 11,
            records = 3, inserted = 2, duplicated = 1, skipped = 2,
        )

        val stored = dao.insert(row)

        assertTrue(stored.id > 0)
        val db = dsl.selectFrom(INGEST_BATCH).where(INGEST_BATCH.ID.eq(stored.id)).fetchOne()!!
        assertEquals("data", db.stream)
        assertEquals(10L, db.firstSeq)
        assertEquals(11L, db.lastSeq)
        assertEquals(3, db.records)
        assertEquals(2, db.inserted)
        assertEquals(1, db.duplicated)
        assertEquals(2, db.skipped)
    }

    @Test
    fun `WHEN the same batch arrives twice THEN both arrivals are kept as separate rows`() {
        val deviceId = insertDevice()
        val row = IngestBatchRow(deviceId, "logs", 5, 1, 32, 32, 32, 0)

        val first = dao.insert(row)
        val second = dao.insert(row.copy(inserted = 0, duplicated = 32))

        assertTrue(second.id > first.id)
        assertEquals(2, dsl.fetchCount(INGEST_BATCH))
    }
}
