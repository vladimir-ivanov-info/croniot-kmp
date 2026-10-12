package com.server.croniot.livelog

import kotlinx.serialization.Serializable

// Where a log line came from. ANDROID/IOS have no producer yet - they are
// here so the admin viewer's filters and the wire format don't change the
// day the apps start shipping logs.
@Serializable
enum class LogPlatform { SERVER, IOT, ANDROID, IOS }

// device_log rows are LOG, device_event rows are EVENT. Server log lines
// are always LOG.
@Serializable
enum class LogKind { LOG, EVENT }

@Serializable
data class LogDeviceRef(val uuid: String, val name: String?)

@Serializable
data class LogOwnerRef(val uuid: String, val nickname: String?, val email: String)

// The one shape the admin viewer renders, whatever the platform. `level`
// uses croniot-iot's ordering (0=Error, 1=Warn, 2=Info, 3=Debug, 4=Trace),
// which is also Logback's, so server and device lines sort and filter
// on the same scale.
//
// `id` is a string prefixed by its source table ("log-12", "evt-7",
// "srv-3", "ing-5") so ids from different tables never collide in the
// viewer's dedupe-by-id between history and the live stream.
@Serializable
data class LogEntryDto(
    val id: String,
    val platform: LogPlatform,
    val kind: LogKind,
    val level: Int,
    val tag: String,
    val message: String,
    val receivedAtMs: Long,
    val device: LogDeviceRef? = null,
    val owner: LogOwnerRef? = null,
    // Device-only.
    val bootId: Long? = null,
    val seq: Long? = null,
    val uptimeMs: Long? = null,
    // Server-only.
    val thread: String? = null,
    val traceId: String? = null,
    val throwable: String? = null,
)

object LogLevels {
    const val ERROR = 0
    const val WARN = 1
    const val INFO = 2
    const val DEBUG = 3
    const val TRACE = 4
}
