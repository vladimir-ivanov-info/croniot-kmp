package com.server.croniot.livelog

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.server.croniot.application.MDC_TRACE_ID
import java.util.concurrent.atomic.AtomicLong

// Ids for server log lines, assigned here rather than by the database:
// a server line is published live the moment it is logged (it must still
// show up when Postgres is the thing that is failing), so it needs its id
// before any INSERT. The persisted copy stores the same id, which is what
// lets the viewer dedupe history against live. Unique across restarts:
// the prefix is this process's start time in base 36.
object ServerLogIds {
    private val processPrefix = System.currentTimeMillis().toString(36)
    private val counter = AtomicLong(0)

    fun next(): String = "srv-$processPrefix-${counter.incrementAndGet()}"
}

// Logback appender (registered in logback.xml) that turns every server log
// line into a LogEntryDto for the live viewer.
//
// Never logs and never blocks: append() runs on whatever thread called the
// logger, and LiveLogBus.publish() is a non-suspending tryEmit. A line
// logged *while* this appender is running on the same thread (nothing does
// today, but a future change might) is dropped instead of recursing.
class LiveLogAppender(
    private val bus: LiveLogBus,
) : AppenderBase<ILoggingEvent>() {

    // Logback instantiates appenders from logback.xml through this one.
    constructor() : this(LiveLogBus.shared)

    private val inAppend = ThreadLocal.withInitial { false }

    override fun append(event: ILoggingEvent) {
        if (inAppend.get()) return
        inAppend.set(true)
        try {
            bus.publish(toEntry(event))
        } finally {
            inAppend.set(false)
        }
    }

    companion object {
        private const val MAX_MESSAGE_CHARS = 4000

        // Ktor's CallLogging colours its "200 OK: GET /x" lines with ANSI
        // escapes, which are noise outside a terminal.
        private val ANSI_ESCAPE = Regex("\u001B\\[[;\\d]*m")

        fun toEntry(event: ILoggingEvent, id: String = ServerLogIds.next()): LogEntryDto = LogEntryDto(
            id = id,
            platform = LogPlatform.SERVER,
            kind = LogKind.LOG,
            level = levelOf(event.level),
            tag = event.loggerName.substringAfterLast('.'),
            message = event.formattedMessage.orEmpty().replace(ANSI_ESCAPE, "").take(MAX_MESSAGE_CHARS),
            receivedAtMs = event.timeStamp,
            thread = event.threadName,
            // runCatching: a LoggerContext without an MDC adapter throws here.
            traceId = runCatching { event.mdcPropertyMap[MDC_TRACE_ID] }.getOrNull(),
            throwable = event.throwableProxy?.let { "${it.className}: ${it.message.orEmpty()}" },
        )

        fun levelOf(level: Level): Int = when (level.toInt()) {
            Level.ERROR_INT -> LogLevels.ERROR
            Level.WARN_INT -> LogLevels.WARN
            Level.INFO_INT -> LogLevels.INFO
            Level.DEBUG_INT -> LogLevels.DEBUG
            else -> LogLevels.TRACE
        }
    }
}
