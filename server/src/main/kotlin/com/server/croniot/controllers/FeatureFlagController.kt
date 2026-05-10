package com.server.croniot.controllers

import com.server.croniot.application.DomainException
import com.server.croniot.application.JwtConfig
import com.server.croniot.mqtt.MqttController
import com.server.croniot.services.FeatureFlagService
import croniot.models.dto.FeatureFlagDto
import croniot.models.errors.DomainError
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class SetFlagRequest(val enabled: Boolean)

class FeatureFlagController @Inject constructor(
    private val service: FeatureFlagService,
) {
    suspend fun getAllFlags(call: ApplicationCall) {
        call.respond(service.getAll())
    }

    suspend fun setFlag(call: ApplicationCall) {
        call.requireAdmin()
        val name = call.parameters["name"]
            ?: throw DomainException(DomainError.Validation("name", "Missing flag name"))
        val body = call.receive<SetFlagRequest>()
        val updated = service.setEnabled(name, body.enabled)
        MqttController.broadcastFeatureFlagUpdate(updated)
        call.respond(updated)
    }

    private fun ApplicationCall.requireAdmin() {
        val isAdmin = principal<JWTPrincipal>()
            ?.payload?.getClaim(JwtConfig.CLAIM_IS_ADMIN)?.asBoolean() ?: false
        if (!isAdmin) throw DomainException(DomainError.Unauthorized("Admin access required"))
    }
}
