package croniot.models.errors

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class DomainErrorTest {

    @Test
    fun `WHEN Unauthorized is created with no message THEN it has code UNAUTHORIZED and a default message`() {
        val error = DomainError.Unauthorized()

        assertThat(error.code).isEqualTo("UNAUTHORIZED")
        assertThat(error.message).isEqualTo("Unauthorized")
    }

    @Test
    fun `WHEN Unauthorized is created with a custom message THEN it keeps code UNAUTHORIZED`() {
        val error = DomainError.Unauthorized(message = "Token expired")

        assertThat(error.code).isEqualTo("UNAUTHORIZED")
        assertThat(error.message).isEqualTo("Token expired")
    }

    @Test
    fun `WHEN InvalidCredentials is created with no message THEN it has code INVALID_CREDENTIALS and a default message`() {
        val error = DomainError.InvalidCredentials()

        assertThat(error.code).isEqualTo("INVALID_CREDENTIALS")
        assertThat(error.message).isEqualTo("Invalid credentials")
    }

    @Test
    fun `WHEN NotFound is created with a resource name THEN it has code NOT_FOUND and builds a default message from it`() {
        val error = DomainError.NotFound(resource = "Account")

        assertThat(error.code).isEqualTo("NOT_FOUND")
        assertThat(error.message).isEqualTo("Account not found")
    }

    @Test
    fun `WHEN NotFound is created with a custom message THEN it overrides the default message`() {
        val error = DomainError.NotFound(resource = "Device", message = "Device is not registered")

        assertThat(error.message).isEqualTo("Device is not registered")
    }

    @Test
    fun `WHEN Validation is created with a field THEN it has code VALIDATION and carries that field`() {
        val error = DomainError.Validation(field = "email", message = "Invalid email format")

        assertThat(error.code).isEqualTo("VALIDATION")
        assertThat(error.field).isEqualTo("email")
        assertThat(error.message).isEqualTo("Invalid email format")
    }

    @Test
    fun `WHEN Conflict is created with an explicit message THEN it has code CONFLICT`() {
        val error = DomainError.Conflict(message = "Email already registered")

        assertThat(error.code).isEqualTo("CONFLICT")
        assertThat(error.message).isEqualTo("Email already registered")
    }

    @Test
    fun `WHEN RateLimited is created with no message THEN it has code RATE_LIMITED and a default message`() {
        val error = DomainError.RateLimited()

        assertThat(error.code).isEqualTo("RATE_LIMITED")
        assertThat(error.message).isEqualTo("Too many requests")
    }

    @Test
    fun `WHEN Internal is created with no message THEN it has code INTERNAL and a default message`() {
        val error = DomainError.Internal()

        assertThat(error.code).isEqualTo("INTERNAL")
        assertThat(error.message).isEqualTo("Internal server error")
    }

    @Test
    fun `WHEN Internal is created with a custom message THEN it uses that message`() {
        val error = DomainError.Internal(message = "Database connection lost")

        assertThat(error.message).isEqualTo("Database connection lost")
    }

    @Test
    fun `WHEN two Unauthorized instances share the same message THEN they are equal`() {
        assertThat(DomainError.Unauthorized("x")).isEqualTo(DomainError.Unauthorized("x"))
    }

    @Test
    fun `WHEN two NotFound instances have different resources THEN they are not equal`() {
        val a = DomainError.NotFound(resource = "Account")
        val b = DomainError.NotFound(resource = "Device")

        assertThat(a).isEqualTo(a)
        assertThat(a == b).isEqualTo(false)
    }
}
