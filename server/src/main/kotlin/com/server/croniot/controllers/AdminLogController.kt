package com.server.croniot.controllers

import com.server.croniot.livelog.LogEntryDto
import com.server.croniot.livelog.LogHistoryDao
import com.server.croniot.livelog.LogHistoryQuery
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

// GET /api/admin/logs (admin only): stored log entries from every
// platform, newest first - see LogHistoryQuery for the parameters. What
// the viewer loads when it opens, when it scrolls back, and when it fills
// a gap reported by the live stream.
class AdminLogController @Inject constructor(
    private val logHistoryDao: LogHistoryDao,
) {
    suspend fun getLogs(call: ApplicationCall) {
        val entries = logHistoryDao.find(LogHistoryQuery.from(call.request.queryParameters))
        // Same JSON as the live stream (null fields omitted), so the viewer
        // parses both with one type.
        call.respondText(
            json.encodeToString(ListSerializer(LogEntryDto.serializer()), entries),
            ContentType.Application.Json
        )
    }

    private companion object {
        val json = Json { explicitNulls = false }
    }
}
