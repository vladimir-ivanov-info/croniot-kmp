package com.server.croniot.services

import com.server.croniot.application.ApplicationScope
import com.server.croniot.jooq.tables.DeviceEvent.Companion.DEVICE_EVENT
import com.server.croniot.jooq.tables.DeviceLog.Companion.DEVICE_LOG
import com.server.croniot.jooq.tables.IngestBatch.Companion.INGEST_BATCH
import com.server.croniot.jooq.tables.ServerLog.Companion.SERVER_LOG
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jooq.DSLContext
import org.jooq.Table
import org.jooq.TableField
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

// How long each log table keeps rows. Defaults follow the observability
// plan (logs 14 days, events 180); each can be overridden with an
// environment variable, e.g. CRONIOT_RETENTION_DEVICE_LOG_DAYS=30.
data class LogRetentionPolicy(
    val deviceLogDays: Long = 14,
    val deviceEventDays: Long = 180,
    val serverLogDays: Long = 30,
    val ingestBatchDays: Long = 90,
) {
    companion object {
        fun fromEnvironment(env: Map<String, String> = System.getenv()): LogRetentionPolicy {
            val defaults = LogRetentionPolicy()
            fun days(name: String, default: Long) = env[name]?.toLongOrNull()?.takeIf { it > 0 } ?: default
            return LogRetentionPolicy(
                deviceLogDays = days("CRONIOT_RETENTION_DEVICE_LOG_DAYS", defaults.deviceLogDays),
                deviceEventDays = days("CRONIOT_RETENTION_DEVICE_EVENT_DAYS", defaults.deviceEventDays),
                serverLogDays = days("CRONIOT_RETENTION_SERVER_LOG_DAYS", defaults.serverLogDays),
                ingestBatchDays = days("CRONIOT_RETENTION_INGEST_BATCH_DAYS", defaults.ingestBatchDays),
            )
        }
    }
}

// Deletes log rows older than their table's retention, once a day. Deletes
// in chunks of CHUNK_SIZE rows so a big first run never holds a long lock
// on a table that MQTT ingestion is writing to. Logs what it removed, so
// each run also shows up in the admin live viewer.
class LogRetentionService(
    private val dsl: DSLContext,
    private val policy: LogRetentionPolicy,
    private val clock: Clock,
) {
    @Inject constructor(dsl: DSLContext) : this(dsl, LogRetentionPolicy.fromEnvironment(), Clock.systemUTC())

    private val logger = KotlinLogging.logger {}

    fun schedule(scope: ApplicationScope, firstRunAfter: Duration = 1.minutes, every: Duration = 24.hours) {
        scope.launch {
            delay(firstRunAfter)
            while (isActive) {
                runCatching { runOnce() }.onFailure { logger.warn(it) { "Log retention run failed" } }
                delay(every)
            }
        }
    }

    // Returns rows deleted per table name.
    fun runOnce(): Map<String, Int> {
        val deleted = linkedMapOf(
            "device_log" to purge(DEVICE_LOG, DEVICE_LOG.ID, DEVICE_LOG.RECEIVED_AT, policy.deviceLogDays),
            "device_event" to purge(DEVICE_EVENT, DEVICE_EVENT.ID, DEVICE_EVENT.RECEIVED_AT, policy.deviceEventDays),
            "server_log" to purge(SERVER_LOG, SERVER_LOG.ID, SERVER_LOG.CREATED_AT, policy.serverLogDays),
            "ingest_batch" to purge(INGEST_BATCH, INGEST_BATCH.ID, INGEST_BATCH.RECEIVED_AT, policy.ingestBatchDays),
        )
        logger.info { "Log retention: deleted ${deleted.entries.joinToString { "${it.value} ${it.key}" }} ($policy)" }
        return deleted
    }

    private fun purge(
        table: Table<*>,
        id: TableField<*, Long?>,
        timestamp: TableField<*, OffsetDateTime?>,
        days: Long,
    ): Int {
        val cutoff = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).minusDays(days)
        var total = 0
        while (true) {
            val chunk = dsl.deleteFrom(table)
                .where(id.`in`(dsl.select(id).from(table).where(timestamp.lt(cutoff)).limit(CHUNK_SIZE)))
                .execute()
            total += chunk
            if (chunk < CHUNK_SIZE) return total
        }
    }

    companion object {
        const val CHUNK_SIZE = 5_000
    }
}
