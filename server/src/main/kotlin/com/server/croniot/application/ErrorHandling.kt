package com.server.croniot.application

import croniot.messages.MessageFactory
import croniot.models.errors.DomainError
import croniot.models.errors.ErrorResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.util.cio.ChannelWriteException
import io.ktor.utils.io.ClosedWriteChannelException

class DomainException(val error: DomainError) : RuntimeException(error.message)

// Errors are written as JSON text directly instead of through
// ContentNegotiation, which honours the request's Accept header: a client
// that accepts only something else - the admin viewer's SSE request sends
// `Accept: text/event-stream` - would get 406 Not Acceptable instead of the
// real status, and a hidden 401 means the client never renews its token.
suspend fun ApplicationCall.respondError(status: HttpStatusCode, body: ErrorResponse) {
    respondText(
        MessageFactory.json.encodeToString(ErrorResponse.serializer(), body),
        ContentType.Application.Json,
        status
    )
}

// The JWT challenge: missing, invalid or expired access token.
suspend fun ApplicationCall.respondInvalidToken() = respondError(
    HttpStatusCode.Unauthorized,
    ErrorResponse(code = DomainError.Unauthorized.CODE, message = "Invalid or expired token"),
)

fun Application.installStatusPages() {
    val logger = KotlinLogging.logger("StatusPages")

    install(StatusPages) {
        exception<DomainException> { call, cause ->
            val (status, body) = cause.error.toHttp()
            call.respondError(status, body)
        }
        // The client closed the connection mid-response (e.g. an SSE viewer
        // changing filters or navigating away). The response is already
        // started and the channel is gone, so there is nothing to answer.
        exception<ChannelWriteException> { call, _ ->
            logger.debug { "Client disconnected from ${call.request.local.uri}" }
        }
        exception<ClosedWriteChannelException> { call, _ ->
            logger.debug { "Client disconnected from ${call.request.local.uri}" }
        }
        exception<Throwable> { call, cause ->
            logger.error(cause) { "Unhandled exception on ${call.request.local.uri}" }
            call.respondError(
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
