package com.server.croniot.livelog

import io.ktor.http.Parameters

// What GET /api/admin/logs accepts. Same coarse filters as the live
// stream (LiveLogFilter), plus what only makes sense against stored rows:
//   tag      multi-valued, exact
//   q        case-insensitive substring of message or tag
//   fromMs / toMs   inclusive receivedAt range (what the viewer uses to
//                   refill a `dropped` gap)
//   before + beforeId   keyset cursor: entries strictly older than the
//                   last one of the previous page (receivedAt, then id)
//   limit    1..MAX_LIMIT, default DEFAULT_LIMIT
// Results are always newest first.
data class LogHistoryQuery(
    val filter: LiveLogFilter = LiveLogFilter(),
    val tags: Set<String> = emptySet(),
    val text: String? = null,
    val fromMs: Long? = null,
    val toMs: Long? = null,
    val beforeMs: Long? = null,
    val beforeId: String? = null,
    val limit: Int = DEFAULT_LIMIT,
) {
    companion object {
        const val DEFAULT_LIMIT = 200
        const val MAX_LIMIT = 1000

        fun from(parameters: Parameters): LogHistoryQuery = LogHistoryQuery(
            filter = LiveLogFilter.from(parameters),
            tags = parameters.getAll("tag").orEmpty()
                .flatMap { it.split(',') }.map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
            text = parameters["q"]?.trim()?.takeIf { it.isNotEmpty() },
            fromMs = parameters["fromMs"]?.toLongOrNull(),
            toMs = parameters["toMs"]?.toLongOrNull(),
            beforeMs = parameters["before"]?.toLongOrNull(),
            beforeId = parameters["beforeId"]?.takeIf { it.isNotEmpty() },
            limit = (parameters["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT),
        )
    }
}
