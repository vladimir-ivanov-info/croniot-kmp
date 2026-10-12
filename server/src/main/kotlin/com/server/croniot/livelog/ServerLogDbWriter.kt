package com.server.croniot.livelog

import com.server.croniot.jooq.tables.ServerLog.Companion.SERVER_LOG
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jooq.DSLContext
import java.time.Instant
import java.time.ZoneOffset

// Moves server log lines from ServerLogQueue into the server_log table, in
// batches, on a daemon thread of its own - never on the thread that
// logged. If an INSERT fails (Postgres down, ...) that batch is dropped
// and counted, and the failure is reported at most once a minute; the
// lines are still in server.log.
class ServerLogDbWriter(
    private val dsl: DSLContext,
    private val queue: ServerLogQueue = ServerLogQueue.shared,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
    private val pollTimeoutMs: Long = DEFAULT_POLL_TIMEOUT_MS,
) {
    private val logger = KotlinLogging.logger {}

    @Volatile private var running = false
    private var thread: Thread? = null
    private var lastFailureReportMs = 0L

    @Volatile var failedLines: Long = 0
        private set

    fun start() {
        if (running) return
        running = true
        thread = Thread({ while (running) flushOnce() }, "server-log-db-writer").apply {
            isDaemon = true
            start()
        }
    }

    // Stops the thread and writes whatever is still queued.
    fun stop() {
        running = false
        thread?.join(STOP_JOIN_TIMEOUT_MS)
        while (flushOnce() > 0) { /* drain the rest */ }
    }

    // One batch: waits up to pollTimeoutMs for lines, writes what it got.
    // Returns how many lines it took off the queue.
    fun flushOnce(): Int {
        val batch = queue.drain(batchSize, pollTimeoutMs)
        if (batch.isEmpty()) return 0
        runCatching { insert(batch) }.onFailure { e ->
            failedLines += batch.size
            reportFailure(e, batch.size)
        }
        return batch.size
    }

    private fun insert(batch: List<LogEntryDto>) {
        val step = dsl.insertInto(
            SERVER_LOG,
            SERVER_LOG.ENTRY_ID, SERVER_LOG.LEVEL, SERVER_LOG.TAG, SERVER_LOG.MESSAGE,
            SERVER_LOG.THREAD, SERVER_LOG.TRACE_ID, SERVER_LOG.THROWABLE, SERVER_LOG.CREATED_AT,
        )
        for (e in batch) {
            step.values(
                e.id, e.level.toShort(), e.tag, e.message,
                e.thread, e.traceId, e.throwable,
                Instant.ofEpochMilli(e.receivedAtMs).atOffset(ZoneOffset.UTC),
            )
        }
        step.onConflictDoNothing().execute()
    }

    private fun reportFailure(e: Throwable, lines: Int) {
        val now = System.currentTimeMillis()
        if (now - lastFailureReportMs < FAILURE_REPORT_INTERVAL_MS) return
        lastFailureReportMs = now
        // This logger is excluded from ServerLogQueue, so this line goes to
        // the file and the live view but never back into this writer.
        logger.warn(e) { "Could not store $lines server log line(s) in server_log (total lost so far: $failedLines)" }
    }

    companion object {
        const val DEFAULT_BATCH_SIZE = 200
        const val DEFAULT_POLL_TIMEOUT_MS = 1_000L
        private const val STOP_JOIN_TIMEOUT_MS = 2_000L
        private const val FAILURE_REPORT_INTERVAL_MS = 60_000L
    }
}
