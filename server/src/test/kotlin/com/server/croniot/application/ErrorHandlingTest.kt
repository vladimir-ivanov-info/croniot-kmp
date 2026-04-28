package com.server.croniot.application

import croniot.models.errors.DomainError
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ErrorHandlingTest {

    @Test
    fun `Unauthorized maps to 401 with UNAUTHORIZED code and empty details`() {
        val (status, body) = DomainError.Unauthorized("nope").toHttp()
        assertEquals(HttpStatusCode.Unauthorized, status)
        assertEquals(DomainError.Unauthorized.CODE, body.code)
        assertEquals("nope", body.message)
        assertTrue(body.details.isEmpty())
    }

    @Test
    fun `InvalidCredentials maps to 401 with INVALID_CREDENTIALS code`() {
        val (status, body) = DomainError.InvalidCredentials().toHttp()
        assertEquals(HttpStatusCode.Unauthorized, status)
        assertEquals(DomainError.InvalidCredentials.CODE, body.code)
    }

    @Test
    fun `NotFound maps to 404 with NOT_FOUND code and resource-aware message`() {
        val (status, body) = DomainError.NotFound("account").toHttp()
        assertEquals(HttpStatusCode.NotFound, status)
        assertEquals(DomainError.NotFound.CODE, body.code)
        assertEquals("account not found", body.message)
    }

    @Test
    fun `Validation maps to 400 and includes the field in details`() {
        val (status, body) = DomainError.Validation("email", "required").toHttp()
        assertEquals(HttpStatusCode.BadRequest, status)
        assertEquals(DomainError.Validation.CODE, body.code)
        assertEquals(mapOf("field" to "email"), body.details)
    }

    @Test
    fun `Conflict maps to 409`() {
        val (status, body) = DomainError.Conflict("duplicate").toHttp()
        assertEquals(HttpStatusCode.Conflict, status)
        assertEquals(DomainError.Conflict.CODE, body.code)
    }

    @Test
    fun `RateLimited maps to 429`() {
        val (status, body) = DomainError.RateLimited().toHttp()
        assertEquals(HttpStatusCode.TooManyRequests, status)
        assertEquals(DomainError.RateLimited.CODE, body.code)
    }

    @Test
    fun `Internal maps to 500`() {
        val (status, body) = DomainError.Internal().toHttp()
        assertEquals(HttpStatusCode.InternalServerError, status)
        assertEquals(DomainError.Internal.CODE, body.code)
    }

    @Test
    fun `DomainException exposes the wrapped error`() {
        val err = DomainError.Conflict("dup")
        val ex = DomainException(err)
        assertEquals(err, ex.error)
        assertEquals(err.message, ex.message)
    }
}
