package croniot.models.dto

import croniot.serialization.ZonedDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

// One row of `device_log` or `device_event` (plan §11.3/§12.4 PR11) - the
// same shape covers both tables, since croniot-iot's wire contract
// already uses one record shape (`[seq, uptimeMs, level, tag, msg]`) for
// both streams, distinguished only by which MQTT topic/table it landed
// in. `receivedAt` is the server's own ingestion time, not a
// reconstruction of the device's wall-clock time: the actual batch
// envelope croniot-iot sends (`[bootId, stream, firstSeq, count,
// records]`) carries no `now_ms`/`epoch_ms` anchor, unlike the plan's
// original per-batch header sketch in §3.1 - that field was dropped
// during the device-side implementation, so there is no way to derive
// an accurate absolute event time here. Ordering/filtering by
// `receivedAt` (or `seq` within a boot) is what's actually available.
@Serializable
data class DeviceLogEntryDto(
    val id: Long,
    val bootId: Long,
    val seq: Long,
    val uptimeMs: Long,
    val level: Int,
    val tag: String,
    val message: String,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val receivedAt: ZonedDateTime,
)
