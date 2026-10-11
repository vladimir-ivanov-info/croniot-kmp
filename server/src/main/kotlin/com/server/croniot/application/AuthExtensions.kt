package com.server.croniot.application

import croniot.models.errors.DomainError
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

fun ApplicationCall.isAdmin(): Boolean =
    principal<JWTPrincipal>()?.payload?.getClaim(JwtConfig.CLAIM_IS_ADMIN)?.asBoolean() ?: false

// Throws Unauthorized (-> 401, as before) unless the caller's JWT has
// is_admin = true. Must run after authentication.
fun ApplicationCall.requireAdmin() {
    if (!isAdmin()) throw DomainException(DomainError.Unauthorized("Admin access required"))
}

// Route-scoped admin check, for handlers that cannot call requireAdmin()
// themselves before the response starts - an `sse { }` handler runs after
// the 200 and the event-stream headers are already sent. Install it inside
// `authenticate(AUTH_JWT_REALM) { ... }`: it runs once authentication has
// set the principal.
val AdminOnly = createRouteScopedPlugin("AdminOnly") {
    on(AuthenticationChecked) { call -> call.requireAdmin() }
}
