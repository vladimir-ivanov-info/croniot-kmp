package com.server.croniot.application

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.server.croniot.config.Secrets
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JwtConfig @Inject constructor(
    private val secrets: Secrets,
) {
    val issuer: String = secrets.jwtIssuer
    val audience: String = secrets.jwtAudience
    val accessTokenTtl: Duration = Duration.ofMinutes(secrets.jwtAccessTokenTtlMinutes)

    private val currentAlgorithm: Algorithm = Algorithm.HMAC256(secrets.jwtSecretCurrent)
    private val previousAlgorithm: Algorithm? = secrets.jwtSecretPrevious
        ?.takeIf { it.isNotBlank() }
        ?.let { Algorithm.HMAC256(it) }

    fun issueAccessToken(
        accountId: Long,
        email: String,
        now: Instant = Instant.now(Clock.systemUTC()),
    ): IssuedAccessToken {
        val expiresAt = now.plus(accessTokenTtl)
        val token = JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(accountId.toString())
            .withClaim(CLAIM_EMAIL, email)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(expiresAt))
            .sign(currentAlgorithm)
        return IssuedAccessToken(token, expiresAt)
    }

    fun verifier(): JWTVerifier = JWT.require(currentAlgorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun decodeAllowingPrevious(token: String): DecodedJWT? {
        runCatching { return verifier().verify(token) }
        val prev = previousAlgorithm ?: return null
        val prevVerifier = JWT.require(prev)
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
        return runCatching { prevVerifier.verify(token) }.getOrNull()
    }

    data class IssuedAccessToken(val token: String, val expiresAt: Instant)

    companion object {
        const val CLAIM_EMAIL = "email"
    }
}