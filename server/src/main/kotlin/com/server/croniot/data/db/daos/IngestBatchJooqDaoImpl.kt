package com.server.croniot.data.db.daos

import com.server.croniot.jooq.tables.IngestBatch.Companion.INGEST_BATCH
import org.jooq.DSLContext
import java.time.OffsetDateTime
import javax.inject.Inject

class IngestBatchJooqDaoImpl @Inject constructor(
    private val dsl: DSLContext,
) : IngestBatchDao {

    override fun insert(row: IngestBatchRow): StoredIngestBatch {
        val stored = dsl.insertInto(INGEST_BATCH)
            .set(INGEST_BATCH.DEVICE, row.deviceId)
            .set(INGEST_BATCH.STREAM, row.stream)
            .set(INGEST_BATCH.BOOT_ID, row.bootId)
            .set(INGEST_BATCH.FIRST_SEQ, row.firstSeq)
            .set(INGEST_BATCH.LAST_SEQ, row.lastSeq)
            .set(INGEST_BATCH.RECORDS, row.records)
            .set(INGEST_BATCH.INSERTED, row.inserted)
            .set(INGEST_BATCH.DUPLICATED, row.duplicated)
            .set(INGEST_BATCH.SKIPPED, row.skipped)
            .returningResult(INGEST_BATCH.ID, INGEST_BATCH.RECEIVED_AT)
            .fetchOne() ?: error("INSERT ... RETURNING into ingest_batch returned no row")

        return StoredIngestBatch(
            id = stored.value1() ?: error("ingest_batch.id is null"),
            row = row,
            receivedAt = stored.value2() ?: OffsetDateTime.now(),
        )
    }
}
