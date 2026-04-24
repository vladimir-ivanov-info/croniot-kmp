package croniot.models.errors

sealed class DomainError {
    abstract val code: String
    abstract val message: String

    data class Unauthorized(
        override val message: String = "Unauthorized",
    ) : DomainError() {
        override val code: String = CODE
        companion object { const val CODE = "UNAUTHORIZED" }
    }

    data class InvalidCredentials(
        override val message: String = "Invalid credentials",
    ) : DomainError() {
        override val code: String = CODE
        companion object { const val CODE = "INVALID_CREDENTIALS" }
    }

    data class NotFound(
        val resource: String,
        override val message: String = "$resource not found",
    ) : DomainError() {
        override val code: String = CODE
        companion object { const val CODE = "NOT_FOUND" }
    }

    data class Validation(
        val field: String,
        override val message: String,
    ) : DomainError() {
        override val code: String = CODE
        companion object { const val CODE = "VALIDATION" }
    }

    data class Conflict(
        override val message: String,
    ) : DomainError() {
        override val code: String = CODE
        companion object { const val CODE = "CONFLICT" }
    }

    data class RateLimited(
        override val message: String = "Too many requests",
    ) : DomainError() {
        override val code: String = CODE
        companion object { const val CODE = "RATE_LIMITED" }
    }

    data class Internal(
        override val message: String = "Internal server error",
    ) : DomainError() {
        override val code: String = CODE
        companion object { const val CODE = "INTERNAL" }
    }
}