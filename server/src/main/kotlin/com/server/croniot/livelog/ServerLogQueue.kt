package com.server.croniot.livelog

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

// Hand-off between LiveLogAppender (any thread that logs) and
// ServerLogDbWriter (its own thread). Bounded and non-blocking on the
// producer side: when the writer cannot keep up, or the database is down
// for long, new lines are dropped and counted instead of filling memory or
// stalling the caller. They are still in server.log either way.
class ServerLogQueue(capacity: Int = DEFAULT_CAPACITY) {
    private val queue = ArrayBlockingQueue<LogEntryDto>(capacity)
    private val dropped = AtomicLong(0)

    val droppedCount: Long get() = dropped.get()

    fun offer(loggerName: String, entry: LogEntryDto) {
        if (isExcluded(loggerName)) return
        if (!queue.offer(entry)) dropped.incrementAndGet()
    }

    // Waits up to `timeoutMs` for the first entry, then takes whatever else
    // is already queued, up to `max`.
    fun drain(max: Int, timeoutMs: Long): List<LogEntryDto> {
        val first = queue.poll(timeoutMs, TimeUnit.MILLISECONDS) ?: return emptyList()
        val batch = ArrayList<LogEntryDto>(max)
        batch.add(first)
        queue.drainTo(batch, max - 1)
        return batch
    }

    companion object {
        const val DEFAULT_CAPACITY = 10_000

        // Lines these loggers emit can be caused by the database write
        // itself (driver, pool, jOOQ, or the writer reporting a failure).
        // Persisting them could loop forever while Postgres is down, so
        // they go to the file and the live view only.
        private val EXCLUDED_PREFIXES = listOf(
            "org.jooq",
            "com.zaxxer.hikari",
            "org.postgresql",
            "com.server.croniot.livelog.ServerLogDbWriter",
        )

        fun isExcluded(loggerName: String): Boolean = EXCLUDED_PREFIXES.any { loggerName.startsWith(it) }

        val shared: ServerLogQueue by lazy { ServerLogQueue() }
    }
}
