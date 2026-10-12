package com.server.croniot.livelog

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LiveLogBusTest {

    private fun entry(n: Int) = LogEntryDto(
        id = "log-$n",
        platform = LogPlatform.IOT,
        kind = LogKind.LOG,
        level = LogLevels.INFO,
        tag = "TAG",
        message = "m$n",
        receivedAtMs = n.toLong(),
    )

    private suspend fun awaitSubscribers(bus: LiveLogBus, count: Int) {
        withTimeout(5_000) { while (bus.subscriberCount < count) yield() }
    }

    @Test
    fun `WHEN nobody is subscribed THEN publish neither blocks nor fails`() {
        val bus = LiveLogBus(capacity = 2)
        repeat(100) { bus.publish(entry(it)) }
    }

    @Test
    fun `WHEN entries are published THEN a subscriber receives them in order with increasing busSeq`() = runBlocking {
        val bus = LiveLogBus()
        val received = async(start = CoroutineStart.UNDISPATCHED) { bus.messages.take(3).toList() }
        awaitSubscribers(bus, 1)

        bus.publishAll(listOf(entry(1), entry(2), entry(3)))

        val messages = withTimeout(5_000) { received.await() }
        assertEquals(listOf("log-1", "log-2", "log-3"), messages.map { it.entry.id })
        assertEquals(listOf(1L, 2L, 3L), messages.map { it.busSeq })
    }

    @Test
    fun `WHEN two subscribers listen THEN both receive every entry`() = runBlocking {
        val bus = LiveLogBus()
        val first = async(start = CoroutineStart.UNDISPATCHED) { bus.messages.take(2).toList() }
        val second = async(start = CoroutineStart.UNDISPATCHED) { bus.messages.take(2).toList() }
        awaitSubscribers(bus, 2)

        bus.publish(entry(1))
        bus.publish(entry(2))

        assertEquals(listOf("log-1", "log-2"), withTimeout(5_000) { first.await() }.map { it.entry.id })
        assertEquals(listOf("log-1", "log-2"), withTimeout(5_000) { second.await() }.map { it.entry.id })
    }

    @Test
    fun `WHEN a subscriber falls behind the buffer THEN the oldest entries are dropped and busSeq shows the gap`() =
        runBlocking {
            val bus = LiveLogBus(capacity = 2)
            // UNDISPATCHED subscribes immediately but the collector does
            // not run again until this coroutine yields, so all five
            // publishes below land before it reads anything.
            val received = async(start = CoroutineStart.UNDISPATCHED) { bus.messages.take(2).toList() }
            awaitSubscribers(bus, 1)

            (1..5).forEach { bus.publish(entry(it)) }

            val messages = withTimeout(5_000) { received.await() }
            // Only the newest `capacity` survive: 4 and 5. The jump from
            // whatever came before to 4 is what tells the SSE endpoint
            // entries were lost.
            assertEquals(listOf(4L, 5L), messages.map { it.busSeq })
        }
}
