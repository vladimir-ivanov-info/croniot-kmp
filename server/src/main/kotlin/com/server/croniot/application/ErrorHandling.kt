package com.server.croniot.application

import croniot.models.errors.DomainError
import croniot.models.errors.ErrorResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

class DomainException(val error: DomainError) : RuntimeException(error.message)

fun Application.installStatusPages() {
    val logger = KotlinLogging.logger("StatusPages")

    install(StatusPages) {
        exception<DomainException> { call, cause ->
            val (status, body) = cause.error.toHttp()
            call.respond(status, body)
        }
        exception<Throwable> { call, cause ->
            logger.error(cause) { "Unhandled exception on ${call.request.local.uri}" }
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    code = DomainError.Internal.CODE,
                    message = "Unexpected server error",
                ),
            )
        }
    }
}

fun DomainError.toHttp(): Pair<HttpStatusCode, ErrorResponse> {
    val status = when (this) {
        is DomainError.Unauthorized -> HttpStatusCode.Unauthorized
        is DomainError.InvalidCredentials -> HttpStatusCode.Unauthorized
        is DomainError.NotFound -> HttpStatusCode.NotFound
        is DomainError.Validation -> HttpStatusCode.BadRequest
        is DomainError.Conflict -> HttpStatusCode.Conflict
        is DomainError.RateLimited -> HttpStatusCode.TooManyRequests
        is DomainError.Internal -> HttpStatusCode.InternalServerError
    }
    val details = if (this is DomainError.Validation) mapOf("field" to field) else emptyMap()
    return status to ErrorResponse(code = code, message = message, details = details)
}