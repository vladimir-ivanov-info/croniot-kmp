package com.server.croniot.admin

import com.server.croniot.livelog.LogOwnerRef
import kotlinx.serialization.Serializable

// One device as the admin pages list it: who owns it, whether it is
// connected, and how it has been doing lately.
@Serializable
data class AdminDeviceSummaryDto(
    val uuid: String,
    val name: String?,
    val owner: LogOwnerRef,
    // null: the device has never reported a status.
    val online: Boolean?,
    val bootId: Long?,
    val statusChangedAtMs: Long?,
    // Newest log or event received from it, if any.
    val lastActivityMs: Long?,
    // Error-level logs + events in the last 24 h.
    val errors24h: Long,
)

@Serializable
data class AdminDeviceDetailDto(
    val summary: AdminDeviceSummaryDto,
    val description: String?,
    // Logs + events per level (0=Error .. 4=Trace) in the last 24 h.
    // Levels with no entries are absent.
    val levelCounts24h: Map<Int, Long>,
    // From ingest_batch, last 24 h: records received vs. how many of them
    // were duplicates of records already stored.
    val recordsReceived24h: Long,
    val recordsDuplicated24h: Long,
)

@Serializable
data class AdminAccountDetailDto(
    val owner: LogOwnerRef,
    val devices: List<AdminDeviceSummaryDto>,
)
