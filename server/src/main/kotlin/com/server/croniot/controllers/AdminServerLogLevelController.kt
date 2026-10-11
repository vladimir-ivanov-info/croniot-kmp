package com.server.croniot.controllers

import com.server.croniot.admin.ServerLogLevelService
import com.server.croniot.admin.SetServerLogLevelRequest
import com.server.croniot.application.DomainException
import croniot.models.errors.DomainError
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import javax.inject.Inject

//   GET    /api/admin/server/log_level   current levels and when they revert
//   PUT    /api/admin/server/log_level   {level, logger?, ttlMinutes?}
//   DELETE /api/admin/server/log_level   restore the original levels now
class AdminServerLogLevelController @Inject constructor(
    private val service: ServerLogLevelService,
) {
    suspend fun get(call: ApplicationCall) = call.respond(service.current())

    suspend fun set(call: ApplicationCall) {
        val request = call.receive<SetServerLogLevelRequest>()
        if (ServerLogLevelService.parse(request.level) == null) {
            throw DomainException(DomainError.Validation("level", "Use TRACE, DEBUG, INFO, WARN or ERROR"))
        }
        call.respond(service.set(request))
    }

    suspend fun revert(call: ApplicationCall) = call.respond(service.revert())
}
