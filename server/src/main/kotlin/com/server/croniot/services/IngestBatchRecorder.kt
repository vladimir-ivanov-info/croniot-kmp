package com.server.croniot.services

import com.server.croniot.data.db.daos.IngestBatchDao
import com.server.croniot.data.db.daos.IngestBatchRow
import com.server.croniot.livelog.DeviceContextLookup
import com.server.croniot.livelog.IngestBatchEntries
import com.server.croniot.livelog.LiveLogBus
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Metrics
import javax.inject.Inject

// Records every batch a device sent into ingest_batch, so resends are
// measurable instead of silently absorbed by the unique keys. When a
// batch carried duplicates it also bumps the
// croniot_ingest_duplicates_total{stream} counter and shows a
// "Resent batch" line in the live viewer.
//
// Never throws: the audit row is secondary to the data itself, so a
// failure here must not stop the device from getting its ack.
class IngestBatchRecorder @Inject constructor(
    private val ingestBatchDao: IngestBatchDao,
    private val bus: LiveLogBus,
    private val deviceContextLookup: DeviceContextLookup,
) {
    private val logger = KotlinLogging.logger {}

    fun record(row: IngestBatchRow) {
        val stored = runCatching { ingestBatchDao.insert(row) }.getOrElse { e ->
            logger.warn(e) { "Could not record ingest batch (device=${row.deviceId} stream=${row.stream})" }
            return
        }
        if (row.duplicated == 0) return

        Metrics.counter(DUPLICATES_METRIC, "stream", row.stream).increment(row.duplicated.toDouble())
        logger.debug { "Resent batch: device=${row.deviceId} stream=${row.stream} duplicated=${row.duplicated}/${row.records}" }

        if (bus.subscriberCount > 0) {
            bus.publish(IngestBatchEntries.toEntry(stored, deviceContextLookup.byDeviceId(row.deviceId)))
        }
    }

    companion object {
        const val DUPLICATES_METRIC = "croniot.ingest.duplicates"
    }
}
