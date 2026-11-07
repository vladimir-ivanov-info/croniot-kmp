package com.server.croniot.ota

// The device's OTA events (croniot-iot ota/OtaController.cpp) turned into
// the status of an update row. Pure, so every branch is unit-tested.
//
// A device event's message is its fields as `key=value key2=value2`
// (croniot-iot log/Log.h). Values may contain spaces ("download: http
// 404"), so a value runs until the next ` key=`.
//
// The device cuts tags to 15 characters (croniot-iot log/LogRecord.h,
// kMaxTagLen), so "ota_download_started" arrives as "ota_download_st":
// names are matched on what survives the cut.
object OtaEvents {
    const val FIRMWARE_INFO = "firmware_info"

    // Still going; the row's finished_at stays NULL.
    const val REQUESTED = "requested"
    const val DOWNLOADING = "downloading"
    const val RETRYING = "retrying"
    const val INSTALLED = "installed"
    const val VALIDATING = "validating"
    const val ROLLING_BACK = "rolling_back"
    const val PROBING = "probing"

    // Finished.
    const val CONFIRMED = "confirmed"
    const val FAILED = "failed"
    const val ROLLED_BACK = "rolled_back"
    const val IGNORED = "ignored"
    const val CANCELLED = "cancelled"

    private val FIELD = Regex("""(?:^|\s)([A-Za-z_][A-Za-z0-9_]*)=""")

    fun parseFields(message: String): Map<String, String> {
        val matches = FIELD.findAll(message).toList()
        return matches.mapIndexed { i, m ->
            val start = m.range.last + 1
            val end = if (i + 1 < matches.size) matches[i + 1].range.first else message.length
            m.groupValues[1] to message.substring(start, end).trim()
        }.toMap()
    }

    fun isOtaEvent(tag: String) = tag.startsWith("ota_")

    const val DEVICE_TAG_MAX = 15

    private val NAMES = listOf(
        "ota_download_started",
        "ota_progress",
        "ota_installed",
        "ota_validating",
        "ota_confirmed",
        "ota_self_test_failed",
        "ota_validation_failed",
        "ota_rolled_back",
        "ota_probing",
        "ota_deferred",
        "ota_failed",
        "ota_request_ignored",
    )

    // The full event name for a tag as stored, cut or not.
    fun eventName(tag: String): String? = NAMES.firstOrNull { it == tag || it.take(DEVICE_TAG_MAX) == tag }

    data class Transition(
        val status: String,
        val progress: Int?,
        val detail: String?,
        val finished: Boolean,
        val fromVersion: String? = null,
    )

    // What `tag` does to an update currently in `currentStatus`; null when
    // it changes nothing (an unrelated event, or a late one for an update
    // that already finished).
    fun transition(tag: String, fields: Map<String, String>, currentStatus: String, currentProgress: Int): Transition? {
        if (currentStatus in FINISHED) return null
        val reason = fields["reason"]
        val bad = fields["bad"] == "true"
        return when (eventName(tag)) {
            "ota_download_started" -> Transition(DOWNLOADING, 0, null, false, fields["from"])
            "ota_progress" -> {
                if (currentStatus !in setOf(REQUESTED, DOWNLOADING, RETRYING)) return null
                val percent = fields["percent"]?.toIntOrNull()?.coerceIn(0, 100) ?: return null
                Transition(DOWNLOADING, maxOf(percent, if (currentStatus == DOWNLOADING) currentProgress else 0), null, false)
            }
            "ota_installed" -> Transition(INSTALLED, 100, "written, restarting into it", false, fields["from"])
            "ota_validating" -> Transition(VALIDATING, 100, "running, checking it works", false)
            "ota_confirmed" -> Transition(CONFIRMED, 100, null, true)
            "ota_self_test_failed" -> Transition(ROLLING_BACK, 100, "self-test ${fields["test"] ?: "?"} failed", false)
            "ota_validation_failed" -> Transition(ROLLING_BACK, 100, describe(reason), false)
            "ota_rolled_back" ->
                if (bad) {
                    Transition(ROLLED_BACK, 100, describe(reason), true)
                } else {
                    Transition(RETRYING, 0, "rolled back (${describe(reason)}), will try again", false)
                }
            "ota_probing" -> Transition(PROBING, 100, "back on the previous firmware to test the connection", false)
            "ota_deferred" ->
                if (bad) {
                    Transition(FAILED, currentProgress, "neither firmware reached the server", true)
                } else {
                    Transition(RETRYING, 0, "neither firmware reached the server: will try again", false)
                }
            "ota_failed" -> failed(reason, currentProgress)
            "ota_request_ignored" -> Transition(IGNORED, currentProgress, describe(reason), true)
            else -> null
        }
    }

    // A download that broke halfway is retried by the device after a
    // while; anything else (hash, size, wrong image, transport) is final.
    private fun failed(reason: String?, progress: Int): Transition =
        if (reason != null && reason.startsWith("download")) {
            Transition(RETRYING, progress, "$reason: will try again", false)
        } else {
            Transition(FAILED, progress, describe(reason), true)
        }

    private fun describe(reason: String?): String = when (reason) {
        null, "" -> "no reason given"
        "boot" -> "the new firmware did not start"
        "self_test" -> "a self-test failed"
        "no_connectivity" -> "the new firmware could not reach the server"
        "new_firmware_no_connectivity" -> "the new firmware could not reach the server"
        "sha256" -> "the download does not match the uploaded file"
        "size" -> "the download has the wrong size"
        "image" -> "not a valid firmware image"
        "version" -> "the image has a different version than requested"
        "transport" -> "updates only work over WiFi"
        "same_version" -> "already running this version"
        "bad_release" -> "this release failed before on the device"
        "attempts_exhausted" -> "no attempts left"
        else -> reason
    }

    private val FINISHED = setOf(CONFIRMED, FAILED, ROLLED_BACK, IGNORED, CANCELLED)

    fun isFinished(status: String) = status in FINISHED
}
