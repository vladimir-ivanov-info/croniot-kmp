package com.server.croniot.livelog

import io.ktor.http.Parameters

// The coarse filters both the live stream and (later) the history endpoint
// apply on the server, so a device in DEBUG doesn't flood a viewer that
// only wants errors. Every field is multi-valued, and an empty set means
// "no filter" - so with no query parameters at all, everything matches.
// Free-text search is left to the browser.
data class LiveLogFilter(
    val platforms: Set<LogPlatform> = emptySet(),
    val kinds: Set<LogKind> = emptySet(),
    val levels: Set<Int> = emptySet(),
    val devices: Set<String> = emptySet(),
    val owners: Set<String> = emptySet(),
) {
    fun matches(entry: LogEntryDto): Boolean =
        (platforms.isEmpty() || entry.platform in platforms) &&
            (kinds.isEmpty() || entry.kind in kinds) &&
            (levels.isEmpty() || entry.level in levels) &&
            (devices.isEmpty() || entry.device?.uuid in devices) &&
            (owners.isEmpty() || entry.owner?.uuid in owners)

    companion object {
        // Accepts both repeated (`level=0&level=1`) and comma-separated
        // (`level=0,1`) values. Unknown values are ignored rather than
        // rejected, so an older server never breaks a newer viewer.
        fun from(parameters: Parameters): LiveLogFilter = LiveLogFilter(
            platforms = values(parameters, "platform").mapNotNull { v ->
                LogPlatform.entries.find { it.name.equals(v, ignoreCase = true) }
            }.toSet(),
            kinds = values(parameters, "kind").mapNotNull { v ->
                LogKind.entries.find { it.name.equals(v, ignoreCase = true) }
            }.toSet(),
            levels = values(parameters, "level").mapNotNull { levelOf(it) }.toSet(),
            devices = values(parameters, "device").toSet(),
            owners = values(parameters, "owner").toSet(),
        )

        private fun values(parameters: Parameters, name: String): List<String> =
            parameters.getAll(name).orEmpty()
                .flatMap { it.split(',') }
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        private fun levelOf(raw: String): Int? = raw.toIntOrNull()?.takeIf { it in LogLevels.ERROR..LogLevels.TRACE }
            ?: when (raw.lowercase()) {
                "error" -> LogLevels.ERROR
                "warn" -> LogLevels.WARN
                "info" -> LogLevels.INFO
                "debug" -> LogLevels.DEBUG
                "trace" -> LogLevels.TRACE
                else -> null
            }
    }
}
