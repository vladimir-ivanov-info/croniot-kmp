package com.server.croniot.testsupport

import com.server.croniot.config.Secrets

object Fixtures {

    const val DEFAULT_JWT_SECRET_CURRENT = "test-secret-current-must-be-long-enough-for-hmac"
    const val DEFAULT_JWT_SECRET_PREVIOUS = "test-secret-previous-must-be-long-enough-for-hmac"

    fun secrets(
        jwtSecretCurrent: String = DEFAULT_JWT_SECRET_CURRENT,
        jwtSecretPrevious: String? = null,
        jwtIssuer: String = "croniot-test-issuer",
        jwtAudience: String = "croniot-test-audience",
        jwtAccessTokenTtlMinutes: Long = 15L,
        jwtRefreshTokenTtlDays: Long = 30L,
    ): Secrets = Secrets(
        mqttBrokerUrl = "tcp://localhost:1883",
        mqttClientId = "test-client",
        databaseUrl = "jdbc:postgresql://localhost:5432/test",
        databaseUser = "test",
        databasePassword = "test",
        keystoreUser = "",
        keystorePassword = "",
        jwtSecretCurrent = jwtSecretCurrent,
        jwtSecretPrevious = jwtSecretPrevious,
        jwtIssuer = jwtIssuer,
        jwtAudience = jwtAudience,
        jwtAccessTokenTtlMinutes = jwtAccessTokenTtlMinutes,
        jwtRefreshTokenTtlDays = jwtRefreshTokenTtlDays,
        dbPoolMaxSize = 4,
        dbPoolMinIdle = 1,
    )
}
