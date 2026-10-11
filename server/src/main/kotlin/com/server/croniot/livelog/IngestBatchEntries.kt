package com.server.croniot.livelog

import com.server.croniot.data.db.daos.StoredIngestBatch

// How a resent batch looks in the viewer. Shared by the live path
// (IngestBatchRecorder) and, later, the history endpoint reading
// ingest_batch, so both render the same line with the same id.
object IngestBatchEntries {
    const val TAG = "Ingest"

    fun toEntry(stored: StoredIngestBatch, context: DeviceContext?): LogEntryDto {
        val row = stored.row
        val seqRange = if (row.firstSeq == row.lastSeq) "seq ${row.firstSeq}" else "seq ${row.firstSeq}-${row.lastSeq}"
        return LogEntryDto(
            id = "ing-${stored.id}",
            platform = LogPlatform.SERVER,
            kind = LogKind.LOG,
            level = LogLevels.INFO,
            tag = TAG,
            message = "Resent batch: ${row.duplicated}/${row.records} duplicate records " +
                "(stream=${row.stream}, boot=${row.bootId}, $seqRange)",
            receivedAtMs = stored.receivedAt.toInstant().toEpochMilli(),
            device = context?.device,
            owner = context?.owner,
            bootId = row.bootId,
        )
    }
}
