package com.server.croniot.application

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.server.croniot.testsupport.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.Date

class JwtConfigTest {

    private val now: Instant = Instant.now()

    @Test
    fun `issues access token with issuer, audience, subject, email claim and expiry`() {
        val secrets = Fixtures.secrets(jwtAccessTokenTtlMinutes = 15L)
        val config = JwtConfig(secrets)

        val issued = config.issueAccessToken(accountId = 42L, email = "user@example.com", now = now)

        val decoded = config.verifier().verify(issued.token)
        assertEquals(secrets.jwtIssuer, decoded.issuer)
        assertEquals(listOf(secrets.jwtAudience), decoded.audience)
        assertEquals("42", decoded.subject)
        assertEquals("user@example.com", decoded.getClaim(JwtConfig.CLAIM_EMAIL).asString())
        assertEquals(now.plus(Duration.ofMinutes(15)).epochSecond, issued.expiresAt.epochSecond)
    }

    @Test
    fun `verifier rejects token signed with a different secret`() {
        val config = JwtConfig(Fixtures.secrets(jwtSecretCurrent = "secret-A-long-enough"))

        val foreignToken = JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject("1")
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plus(Duration.ofMinutes(5))))
            .sign(Algorithm.HMAC256("secret-B-long-enough-different"))

        assertThrows(JWTVerificationException::class.java) {
            config.verifier().verify(foreignToken)
        }
    }

    @Test
    fun `decodeAllowingPrevious accepts tokens signed with previous secret`() {
        val previous = "secret-prev-long-enough-for-hmac"
        val current = "secret-current-long-enough-for-hmac"
        val config = JwtConfig(
            Fixtures.secrets(jwtSecretCurrent = current, jwtSecretPrevious = previous),
        )

        val legacyToken = JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject("7")
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plus(Duration.ofMinutes(5))))
            .sign(Algorithm.HMAC256(previous))

        val decoded = config.decodeAllowingPrevious(legacyToken)
        assertNotNull(decoded)
        assertEquals("7", decoded!!.subject)
    }

    @Test
    fun `decodeAllowingPrevious returns null when token is signed by an unknown secret`() {
        val config = JwtConfig(
            Fixtures.secrets(
                jwtSecretCurrent = "secret-current-long-enough-for-hmac",
                jwtSecretPrevious = "secret-prev-long-enough-for-hmac",
            ),
        )

        val strangerToken = JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject("9")
            .withExpiresAt(Date.from(now.plus(Duration.ofMinutes(5))))
            .sign(Algorithm.HMAC256("stranger-secret-long-enough-for-hmac"))

        assertNull(config.decodeAllowingPrevious(strangerToken))
    }

    @Test
    fun `decodeAllowingPrevious returns null when no previous secret is configured and current fails`() {
        val config = JwtConfig(
            Fixtures.secrets(
                jwtSecretCurrent = "secret-current-long-enough-for-hmac",
                jwtSecretPrevious = null,
            ),
        )

        val foreignToken = JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject("3")
            .withExpiresAt(Date.from(now.plus(Duration.ofMinutes(5))))
            .sign(Algorithm.HMAC256("other-secret-long-enough-for-hmac"))

        assertNull(config.decodeAllowingPrevious(foreignToken))
    }
}
