package com.server.croniot.controllers

import com.server.croniot.livelog.LiveLogBus
import com.server.croniot.livelog.LiveLogFilter
import com.server.croniot.livelog.LiveLogMessage
import com.server.croniot.livelog.LogEntryDto
import io.ktor.server.sse.ServerSSESession
import io.ktor.server.sse.heartbeat
import io.ktor.sse.ServerSentEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

// Sent when this connection missed entries (it fell behind the bus
// buffer). `fromMs`/`toMs` bound the gap in receivedAt time - the last
// entry delivered before it and the first after it - so the viewer can
// fetch exactly that range from the history endpoint and fill it in.
// `count` covers every platform, not just the ones this connection
// filters for.
@Serializable
data class DroppedNotice(val count: Long, val fromMs: Long, val toMs: Long)

// Watches busSeq, which LiveLogBus increments for every entry. A jump
// means this subscriber lost the entries in between.
class LiveLogGapTracker {
    private var lastSeq: Long? = null
    private var lastReceivedAtMs: Long? = null

    fun onMessage(message: LiveLogMessage): DroppedNotice? {
        val previousSeq = lastSeq
        val previousAt = lastReceivedAtMs
        lastSeq = message.busSeq
        lastReceivedAtMs = message.entry.receivedAtMs

        if (previousSeq == null || message.busSeq == previousSeq + 1) return null
        return DroppedNotice(
            count = message.busSeq - previousSeq - 1,
            fromMs = previousAt ?: message.entry.receivedAtMs,
            toMs = message.entry.receivedAtMs,
        )
    }
}

// GET /api/admin/logs/stream (admin only): Server-Sent Events with every
// new log entry that passes the query's filters (see LiveLogFilter).
//   event: log      data: LogEntryDto (JSON)       id: the entry id
//   event: dropped  data: DroppedNotice (JSON)
// A heartbeat goes out every 15 s, so idle proxies keep the connection
// open.
class AdminLogStreamController @Inject constructor(
    private val bus: LiveLogBus,
) {
    suspend fun stream(session: ServerSSESession) {
        val filter = LiveLogFilter.from(session.call.request.queryParameters)
        val gaps = LiveLogGapTracker()
        session.heartbeat { period = HEARTBEAT_PERIOD }

        bus.messages.collect { message ->
            gaps.onMessage(message)?.let { notice ->
                session.send(ServerSentEvent(data = json.encodeToString(DroppedNotice.serializer(), notice), event = "dropped"))
            }
            if (filter.matches(message.entry)) {
                session.send(
                    ServerSentEvent(
                        data = json.encodeToString(LogEntryDto.serializer(), message.entry),
                        event = "log",
                        id = message.entry.id,
                    ),
                )
            }
        }
    }

    private companion object {
        val HEARTBEAT_PERIOD = 15.seconds

        // Null fields are omitted: a server line carries no device fields
        // and vice versa.
        val json = Json { explicitNulls = false }
    }
}
