package com.server.croniot.services

import com.server.croniot.application.ApplicationScope
import com.server.croniot.application.DomainException
import com.server.croniot.application.JwtConfig
import com.server.croniot.data.db.daos.VerifyPasswordResult
import com.server.croniot.data.mappers.toDto
import com.server.croniot.data.repositories.AccountRepository
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.DeviceTokenRepository
import com.server.croniot.mqtt.MqttController
import croniot.messages.LoginDto
import croniot.models.Device
import croniot.models.LoginResultDto
import croniot.models.RefreshTokenResultDto
import croniot.models.Result
import croniot.models.errors.DomainError
import kotlinx.coroutines.launch
import javax.inject.Inject

class LoginService @Inject constructor(
    private val accountRepository: AccountRepository,
    private val deviceRepository: DeviceRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val jwtConfig: JwtConfig,
    private val refreshTokenService: RefreshTokenService,
    private val applicationScope: ApplicationScope,
) {

    fun login(loginDto: LoginDto): LoginResultDto {
        val accountEmail = loginDto.email
        val accountPassword = loginDto.password
        val deviceUuid = loginDto.deviceUuid
        val deviceToken = loginDto.deviceToken

        when (accountRepository.verifyPassword(accountEmail, accountPassword)) {
            VerifyPasswordResult.UserNotFound,
            VerifyPasswordResult.Invalid ->
                throw DomainException(DomainError.InvalidCredentials())
            is VerifyPasswordResult.Valid -> Unit
        }

        var device: Device? = null
        deviceToken?.let {
            device = deviceTokenRepository.getDevice(deviceToken)
        }

        if (device != null) {
            throw DomainException(DomainError.Conflict("Device already registered."))
        }

        val accountId = accountRepository.getAccountId(accountEmail)
            ?: throw DomainException(DomainError.NotFound("account"))

        val newDevice = Device(
            uuid = deviceUuid,
            name = deviceUuid, // TODO use actual device name
            iot = false,
        )
        deviceRepository.createDevice(newDevice, accountId)

        applicationScope.launch {
            MqttController.listenToNewDevice(newDevice)
        }

        val account = accountRepository.getAccount(accountEmail)
            ?: throw DomainException(DomainError.Internal("Account fetch failed."))

        val accessToken = jwtConfig.issueAccessToken(accountId, accountEmail)
        val refresh = refreshTokenService.issueForAccount(accountId, deviceUuid)

        return LoginResultDto(
            result = Result(true, ""),
            accountDto = account.toDto(),
            token = accessToken.token,
            refreshToken = refresh,
            accessTokenExpiresAtEpochSeconds = accessToken.expiresAt.epochSecond,
        )
    }

    fun refresh(oldRefreshToken: String): RefreshTokenResultDto {
        val rotated = refreshTokenService.rotate(oldRefreshToken)
            ?: throw DomainException(DomainError.Unauthorized("Invalid refresh token."))

        return RefreshTokenResultDto(
            result = Result(true, ""),
            token = rotated.accessToken,
            refreshToken = rotated.refreshToken,
            accessTokenExpiresAtEpochSeconds = rotated.accessTokenExpiresAtEpochSeconds,
        )
    }

    fun logout(refreshToken: String): Result {
        refreshTokenService.revoke(refreshToken)
        return Result(true, "")
    }

    fun loginIot(message: LoginDto): Result {
        val accountEmail = message.email
        val accountPassword = message.password
        val deviceToken = message.deviceToken
            ?: throw DomainException(DomainError.Validation("deviceToken", "Missing device token."))

        deviceTokenRepository.getDevice(deviceToken)
            ?: throw DomainException(DomainError.NotFound("device"))

        when (accountRepository.verifyPassword(accountEmail, accountPassword)) {
            VerifyPasswordResult.UserNotFound,
            VerifyPasswordResult.Invalid ->
                throw DomainException(DomainError.InvalidCredentials())
            is VerifyPasswordResult.Valid -> Unit
        }

        accountRepository.getAccountEagerSkipTasks(accountEmail)
            ?: throw DomainException(DomainError.NotFound("account"))

        return Result(true, "Login success")
    }
}