package croniot.models.errors

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class DomainErrorTest {

    @Test
    fun `Unauthorized has code UNAUTHORIZED and a default message`() {
        val error = DomainError.Unauthorized()

        assertThat(error.code).isEqualTo("UNAUTHORIZED")
        assertThat(error.message).isEqualTo("Unauthorized")
    }

    @Test
    fun `Unauthorized accepts a custom message while keeping its code`() {
        val error = DomainError.Unauthorized(message = "Token expired")

        assertThat(error.code).isEqualTo("UNAUTHORIZED")
        assertThat(error.message).isEqualTo("Token expired")
    }

    @Test
    fun `InvalidCredentials has code INVALID_CREDENTIALS and a default message`() {
        val error = DomainError.InvalidCredentials()

        assertThat(error.code).isEqualTo("INVALID_CREDENTIALS")
        assertThat(error.message).isEqualTo("Invalid credentials")
    }

    @Test
    fun `NotFound has code NOT_FOUND and builds a default message from the resource name`() {
        val error = DomainError.NotFound(resource = "Account")

        assertThat(error.code).isEqualTo("NOT_FOUND")
        assertThat(error.message).isEqualTo("Account not found")
    }

    @Test
    fun `NotFound accepts a custom message overriding the default`() {
        val error = DomainError.NotFound(resource = "Device", message = "Device is not registered")

        assertThat(error.message).isEqualTo("Device is not registered")
    }

    @Test
    fun `Validation has code VALIDATION and carries the offending field`() {
        val error = DomainError.Validation(field = "email", message = "Invalid email format")

        assertThat(error.code).isEqualTo("VALIDATION")
        assertThat(error.field).isEqualTo("email")
        assertThat(error.message).isEqualTo("Invalid email format")
    }

    @Test
    fun `Conflict has code CONFLICT and requires an explicit message`() {
        val error = DomainError.Conflict(message = "Email already registered")

        assertThat(error.code).isEqualTo("CONFLICT")
        assertThat(error.message).isEqualTo("Email already registered")
    }

    @Test
    fun `RateLimited has code RATE_LIMITED and a default message`() {
        val error = DomainError.RateLimited()

        assertThat(error.code).isEqualTo("RATE_LIMITED")
        assertThat(error.message).isEqualTo("Too many requests")
    }

    @Test
    fun `Internal has code INTERNAL and a default message`() {
        val error = DomainError.Internal()

        assertThat(error.code).isEqualTo("INTERNAL")
        assertThat(error.message).isEqualTo("Internal server error")
    }

    @Test
    fun `Internal accepts a custom message`() {
        val error = DomainError.Internal(message = "Database connection lost")

        assertThat(error.message).isEqualTo("Database connection lost")
    }

    @Test
    fun `two Unauthorized instances with the same message are equal`() {
        assertThat(DomainError.Unauthorized("x")).isEqualTo(DomainError.Unauthorized("x"))
    }

    @Test
    fun `NotFound instances with different resources are not equal`() {
        val a = DomainError.NotFound(resource = "Account")
        val b = DomainError.NotFound(resource = "Device")

        assertThat(a).isEqualTo(a)
        assertThat(a == b).isEqualTo(false)
    }
}
