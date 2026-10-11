package com.server.croniot.livelog

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicLong

// A LogEntryDto as it travels through the bus. `busSeq` is assigned by
// publish() and is strictly increasing across every platform, so a
// subscriber that sees it jump (e.g. 41 -> 45) knows exactly how many
// entries it missed (3) - the live endpoint turns that into a `dropped`
// notice and the viewer refills the gap from the database.
data class LiveLogMessage(val busSeq: Long, val entry: LogEntryDto)

// In-process fan-out of log entries to whoever is watching live (the
// admin viewer's SSE endpoint). Only ever carries entries that are
// already stored - publishers call publish() after their INSERT
// succeeds - so the live view never shows something history won't.
//
// publish() never blocks and never fails: a slow subscriber must not be
// able to stall MQTT ingestion or a server thread that is logging. With
// DROP_OLDEST, once a subscriber is `capacity` entries behind, the oldest
// ones it hasn't read yet are dropped and its next busSeq jumps.
class LiveLogBus(capacity: Int = DEFAULT_CAPACITY) {
    private val nextSeq = AtomicLong(1)

    private val flow = MutableSharedFlow<LiveLogMessage>(
        replay = 0,
        extraBufferCapacity = capacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val messages: SharedFlow<LiveLogMessage> = flow.asSharedFlow()

    val subscriberCount: Int get() = flow.subscriptionCount.value

    fun publish(entry: LogEntryDto) {
        // tryEmit always succeeds with DROP_OLDEST; with no subscribers
        // the entry is simply not buffered, which is what we want.
        flow.tryEmit(LiveLogMessage(nextSeq.getAndIncrement(), entry))
    }

    fun publishAll(entries: List<LogEntryDto>) {
        entries.forEach(::publish)
    }

    companion object {
        const val DEFAULT_CAPACITY = 2048

        // The process-wide instance. Exposed this way (and handed to
        // Dagger from AppModule) because the Logback appender that feeds
        // server logs into the bus is created by Logback, not by Dagger.
        val shared: LiveLogBus by lazy { LiveLogBus() }
    }
}
