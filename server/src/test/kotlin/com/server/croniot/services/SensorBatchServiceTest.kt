package com.server.croniot.services

import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.SensorDataRepository
import com.server.croniot.data.repositories.SensorTypeRepository
import com.server.croniot.mqtt.SensorBatchEnvelope
import com.server.croniot.mqtt.SensorBatchRecord
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SensorBatchServiceTest {

    private val sensorDataRepository: SensorDataRepository = mockk(relaxUnitFun = true)
    private val sensorTypeRepository: SensorTypeRepository = mockk()
    private val deviceRepository: DeviceRepository = mockk()

    private val service = SensorBatchService(
        sensorDataRepository = sensorDataRepository,
        sensorTypeRepository = sensorTypeRepository,
        deviceRepository = deviceRepository,
    )

    @Test
    fun `WHEN the device is unknown THEN ingestBatch drops the batch and returns null`() {
        every { deviceRepository.getId("unknown-uuid") } returns null

        val envelope = SensorBatchEnvelope(bootId = 1, stream = 2, records = listOf(
            SensorBatchRecord(seq = 1, sensorUid = 42, t0Ms = 0, dtMs = 0, values = listOf(1.0)),
        ))
        val result = service.ingestBatch("unknown-uuid", envelope)

        assertNull(result)
        verify(exactly = 0) { sensorDataRepository.insertBatch(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `WHEN a record's sensorUid is not registered THEN it is skipped but other records still insert`() {
        every { deviceRepository.getId("device-uuid") } returns 7L
        every { sensorTypeRepository.getId(7L, 42L) } returns null
        every { sensorTypeRepository.getId(7L, 99L) } returns 55L
        every { sensorDataRepository.insertBatch(7L, 55L, 1L, 2L, listOf(2.0), any()) } returns 1

        val envelope = SensorBatchEnvelope(bootId = 1, stream = 2, records = listOf(
            SensorBatchRecord(seq = 1, sensorUid = 42, t0Ms = 0, dtMs = 0, values = listOf(1.0)),
            SensorBatchRecord(seq = 2, sensorUid = 99, t0Ms = 0, dtMs = 0, values = listOf(2.0)),
        ))
        val result = service.ingestBatch("device-uuid", envelope)

        assertEquals(LogStream.DATA, result?.stream)
        assertEquals(2L, result?.upToSeq)
        verify(exactly = 0) { sensorDataRepository.insertBatch(7L, any(), 1L, 1L, any(), any()) }
        verify { sensorDataRepository.insertBatch(7L, 55L, 1L, 2L, listOf(2.0), any()) }
    }

    @Test
    fun `WHEN the envelope has no records THEN ingestBatch returns null`() {
        every { deviceRepository.getId("device-uuid") } returns 7L

        val envelope = SensorBatchEnvelope(bootId = 1, stream = 2, records = emptyList())
        val result = service.ingestBatch("device-uuid", envelope)

        assertNull(result)
    }
}
