package com.server.croniot.services

import com.server.croniot.data.repositories.DeviceEventRepository
import com.server.croniot.data.repositories.DeviceLogRepository
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.mqtt.DeviceLogBatch
import com.server.croniot.mqtt.DeviceLogRecord
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DeviceLogServiceTest {

    private val deviceLogRepository: DeviceLogRepository = mockk(relaxUnitFun = true)
    private val deviceEventRepository: DeviceEventRepository = mockk(relaxUnitFun = true)
    private val deviceRepository: DeviceRepository = mockk()

    private val service = DeviceLogService(
        deviceLogRepository = deviceLogRepository,
        deviceEventRepository = deviceEventRepository,
        deviceRepository = deviceRepository,
    )

    private fun record(seq: Long) = DeviceLogRecord(seq = seq, uptimeMs = seq * 1000, level = 2, tag = "TAG", message = "m")

    @Test
    fun `WHEN the device is unknown THEN ingestBatch drops the batch and returns null`() {
        every { deviceRepository.getId("unknown-uuid") } returns null

        val batch = DeviceLogBatch(bootId = 1, stream = 0, firstSeq = 1, count = 1, records = listOf(record(1)))
        val result = service.ingestBatch("unknown-uuid", batch)

        assertNull(result)
        verify(exactly = 0) { deviceLogRepository.insertBatch(any(), any(), any()) }
    }

    @Test
    fun `WHEN the stream is unsupported THEN ingestBatch drops the batch and returns null`() {
        every { deviceRepository.getId("device-uuid") } returns 7L

        // stream=2 is Data - a real, ingestable stream now, but through
        // SensorBatchService, not this one. DeviceLogService only knows
        // device_log/device_event, so it must still reject it.
        val batch = DeviceLogBatch(bootId = 1, stream = 2, firstSeq = 1, count = 1, records = listOf(record(1)))
        val result = service.ingestBatch("device-uuid", batch)

        assertNull(result)
    }

    @Test
    fun `WHEN stream is Logs THEN ingestBatch inserts into the log repository and acks the highest seq`() {
        every { deviceRepository.getId("device-uuid") } returns 7L
        every { deviceLogRepository.insertBatch(7L, 100L, any()) } returns 2

        val batch = DeviceLogBatch(bootId = 100, stream = 0, firstSeq = 5, count = 2, records = listOf(record(5), record(6)))
        val result = service.ingestBatch("device-uuid", batch)

        assertEquals(LogStream.LOGS, result?.stream)
        assertEquals(6L, result?.upToSeq)
        verify { deviceLogRepository.insertBatch(7L, 100L, batch.records) }
        verify(exactly = 0) { deviceEventRepository.insertBatch(any(), any(), any()) }
    }

    @Test
    fun `WHEN stream is Events THEN ingestBatch inserts into the event repository`() {
        every { deviceRepository.getId("device-uuid") } returns 7L
        every { deviceEventRepository.insertBatch(7L, 100L, any()) } returns 1

        val batch = DeviceLogBatch(bootId = 100, stream = 1, firstSeq = 9, count = 1, records = listOf(record(9)))
        val result = service.ingestBatch("device-uuid", batch)

        assertEquals(LogStream.EVENTS, result?.stream)
        assertEquals(9L, result?.upToSeq)
        verify { deviceEventRepository.insertBatch(7L, 100L, batch.records) }
    }

    @Test
    fun `WHEN the batch has zero records THEN ingestBatch still acks using firstSeq`() {
        every { deviceRepository.getId("device-uuid") } returns 7L
        every { deviceLogRepository.insertBatch(7L, 100L, emptyList()) } returns 0

        val batch = DeviceLogBatch(bootId = 100, stream = 0, firstSeq = 42, count = 0, records = emptyList())
        val result = service.ingestBatch("device-uuid", batch)

        assertEquals(42L, result?.upToSeq)
    }
}
