package com.server.croniot.data.db.daos

import java.time.OffsetDateTime

// One received batch, as stored in ingest_batch (see db/migration).
data class IngestBatchRow(
    val deviceId: Long,
    val stream: String,
    val bootId: Long,
    val firstSeq: Long,
    val lastSeq: Long,
    val records: Int,
    val inserted: Int,
    val duplicated: Int,
    val skipped: Int = 0,
)

data class StoredIngestBatch(val id: Long, val row: IngestBatchRow, val receivedAt: OffsetDateTime)

interface IngestBatchDao {
    fun insert(row: IngestBatchRow): StoredIngestBatch
}
