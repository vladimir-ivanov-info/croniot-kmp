package com.server.croniot.services

import com.server.croniot.application.ApplicationScope
import com.server.croniot.application.DomainException
import com.server.croniot.application.JwtConfig
import com.server.croniot.data.db.daos.VerifyPasswordResult
import com.server.croniot.data.repositories.AccountRepository
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.DeviceTokenRepository
import com.server.croniot.mqtt.MqttController
import com.server.croniot.testsupport.Fixtures
import croniot.messages.LoginDto
import croniot.models.Account
import croniot.models.Device
import croniot.models.errors.DomainError
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LoginServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val deviceRepository: DeviceRepository = mockk(relaxUnitFun = true)
    private val deviceTokenRepository: DeviceTokenRepository = mockk()
    private val refreshTokenService: RefreshTokenService = mockk()
    private val applicationScope = ApplicationScope()
    private val jwtConfig = JwtConfig(Fixtures.secrets())

    private val service = LoginService(
        accountRepository = accountRepository,
        deviceRepository = deviceRepository,
        deviceTokenRepository = deviceTokenRepository,
        jwtConfig = jwtConfig,
        refreshTokenService = refreshTokenService,
        applicationScope = applicationScope,
    )

    private val loginDto = LoginDto(
        email = "user@example.com",
        password = "secret",
        deviceUuid = "device-uuid",
        deviceToken = null,
        deviceProperties = emptyMap(),
    )

    @BeforeEach
    fun setUp() {
        mockkObject(MqttController)
        every { MqttController.listenToNewDevice(any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(MqttController)
        applicationScope.shutdown()
    }

    @Test
    fun `login throws InvalidCredentials when password does not match`() {
        every { accountRepository.verifyPassword(any(), any()) } returns VerifyPasswordResult.Invalid

        val ex = assertThrows(DomainException::class.java) { service.login(loginDto) }
        assertInstanceOf(DomainError.InvalidCredentials::class.java, ex.error)
    }

    @Test
    fun `login throws InvalidCredentials when user does not exist`() {
        every { accountRepository.verifyPassword(any(), any()) } returns VerifyPasswordResult.UserNotFound

        val ex = assertThrows(DomainException::class.java) { service.login(loginDto) }
        assertInstanceOf(DomainError.InvalidCredentials::class.java, ex.error)
    }

    @Test
    fun `login throws Conflict when deviceToken resolves to a known device`() {
        every { accountRepository.verifyPassword(any(), any()) } returns VerifyPasswordResult.Valid(rehashed = false)
        every { deviceTokenRepository.getDevice("token") } returns Device(uuid = "d", name = "d", iot = true)

        val ex = assertThrows(DomainException::class.java) {
            service.login(loginDto.copy(deviceToken = "token"))
        }
        assertInstanceOf(DomainError.Conflict::class.java, ex.error)
    }

    @Test
    fun `login throws NotFound when accountId lookup returns null`() {
        every { accountRepository.verifyPassword(any(), any()) } returns VerifyPasswordResult.Valid(rehashed = false)
        every { accountRepository.getAccountId(any()) } returns null

        val ex = assertThrows(DomainException::class.java) { service.login(loginDto) }
        assertInstanceOf(DomainError.NotFound::class.java, ex.error)
    }

    @Test
    fun `login throws Internal when account fetch fails after device creation`() {
        every { accountRepository.verifyPassword(any(), any()) } returns VerifyPasswordResult.Valid(rehashed = false)
        every { accountRepository.getAccountId("user@example.com") } returns 77L
        every { deviceRepository.createDevice(any(), 77L) } returns 1L
        every { accountRepository.getAccount("user@example.com") } returns null

        val ex = assertThrows(DomainException::class.java) { service.login(loginDto) }
        assertInstanceOf(DomainError.Internal::class.java, ex.error)
    }

    @Test
    fun `login happy path returns tokens, issues refresh and creates device`() {
        val account = Account(uuid = "acc-uuid", nickname = "nick", email = "user@example.com", devices = emptyList())
        every { accountRepository.verifyPassword(any(), any()) } returns VerifyPasswordResult.Valid(rehashed = false)
        every { accountRepository.getAccountId("user@example.com") } returns 77L
        every { deviceRepository.createDevice(any(), 77L) } returns 1L
        every { accountRepository.getAccount("user@example.com") } returns account
        every { refreshTokenService.issueForAccount(77L, "device-uuid") } returns "refresh-token-plaintext"

        val result = service.login(loginDto)

        assertTrue(result.result.success)
        assertTrue(!result.token.isNullOrBlank())
        assertEquals("refresh-token-plaintext", result.refreshToken)
        assertEquals(account.email, result.accountDto?.email)
        verify(exactly = 1) { deviceRepository.createDevice(any(), 77L) }
    }

    @Test
    fun `refresh returns tokens when rotation succeeds`() {
        every { refreshTokenService.rotate("old") } returns RefreshTokenService.RotationResult(
            accessToken = "new-access",
            accessTokenExpiresAtEpochSeconds = 1_000_000L,
            refreshToken = "new-refresh",
        )

        val result = service.refresh("old")

        assertTrue(result.result.success)
        assertEquals("new-access", result.token)
        assertEquals("new-refresh", result.refreshToken)
        assertEquals(1_000_000L, result.accessTokenExpiresAtEpochSeconds)
    }

    @Test
    fun `refresh throws Unauthorized when rotation returns null`() {
        every { refreshTokenService.rotate(any()) } returns null

        val ex = assertThrows(DomainException::class.java) { service.refresh("old") }
        assertInstanceOf(DomainError.Unauthorized::class.java, ex.error)
    }

    @Test
    fun `logout revokes the refresh token and returns success`() {
        every { refreshTokenService.revoke("plaintext") } returns Unit

        val result = service.logout("plaintext")

        assertTrue(result.success)
        verify(exactly = 1) { refreshTokenService.revoke("plaintext") }
    }

    @Test
    fun `loginIot throws Validation when deviceToken is missing`() {
        val ex = assertThrows(DomainException::class.java) {
            service.loginIot(loginDto.copy(deviceToken = null))
        }
        assertInstanceOf(DomainError.Validation::class.java, ex.error)
    }

    @Test
    fun `loginIot throws NotFound when deviceToken does not resolve to a device`() {
        every { deviceTokenRepository.getDevice(any()) } returns null

        val ex = assertThrows(DomainException::class.java) {
            service.loginIot(loginDto.copy(deviceToken = "token"))
        }
        assertInstanceOf(DomainError.NotFound::class.java, ex.error)
    }

    @Test
    fun `loginIot throws InvalidCredentials when password verification fails`() {
        every { deviceTokenRepository.getDevice(any()) } returns Device(uuid = "d", name = "d", iot = true)
        every { accountRepository.verifyPassword(any(), any()) } returns VerifyPasswordResult.Invalid

        val ex = assertThrows(DomainException::class.java) {
            service.loginIot(loginDto.copy(deviceToken = "token"))
        }
        assertInstanceOf(DomainError.InvalidCredentials::class.java, ex.error)
    }

}
