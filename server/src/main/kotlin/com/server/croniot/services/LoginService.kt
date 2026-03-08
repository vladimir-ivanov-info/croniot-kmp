package com.server.croniot.services

import com.server.croniot.data.repositories.AccountRepository
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.DeviceTokenRepository
import com.server.croniot.mqtt.MqttController
import croniot.messages.LoginDto
import croniot.models.Device
import croniot.models.LoginResultDto
import croniot.models.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.server.croniot.data.mappers.toDto

class LoginService @Inject constructor(
    private val accountRepository: AccountRepository,
    private val deviceRepository: DeviceRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
) {

    fun login(loginDto: LoginDto): LoginResultDto {
        val accountEmail = loginDto.email
        val deviceUuid = loginDto.deviceUuid
        val deviceToken = loginDto.deviceToken

        // TODO validate email and password
        val accountExists = accountRepository.isAccountExists(accountEmail)
        if (!accountExists) {
            return LoginResultDto(Result(false, ""), null, null)
        }

        var device: Device? = null
        deviceToken?.let {
            device = deviceTokenRepository.getDevice(deviceToken)
        }

        if (device != null) {
            return LoginResultDto(Result(false, ""), null, null)
        }

        val newDevice = Device(
            uuid = deviceUuid,
            name = deviceUuid, //TODO use actual device name
            iot = false,
        )

        val accountId = accountRepository.getAccountId(accountEmail)
            ?: return LoginResultDto(Result(false, ""), null, null)

        deviceRepository.createDevice(newDevice, accountId)

        CoroutineScope(Dispatchers.IO).launch {
            MqttController.listenToNewDevice(newDevice)
        }

        val newToken = Global.generateUniqueString(8)
        //TODO deviceTokenRepository.createDeviceToken(newDevice, newToken)

        val account = accountRepository.getAccount(accountEmail)
            ?: return LoginResultDto(Result(false, ""), null, null)

        return LoginResultDto(
            result = Result(true, ""),
            accountDto = account.toDto(),
            token = newToken
        )
    }

    fun loginIot(message: LoginDto): Result {
        val accountEmail = message.email
        val accountPassword = message.password
        val deviceToken = message.deviceToken

        if (deviceToken == null) {
            return Result(false, "Login failed: no token provided.")
        }

        val device = deviceTokenRepository.getDevice(deviceToken)
            ?: return Result(false, "Login failed: no device found for given token.")

        // TODO avoid fetching full account graph just to check existence
        val account = accountRepository.getAccountEagerSkipTasks(accountEmail, accountPassword)
            ?: return Result(false, "Login failed.")

        return Result(true, "Login success")
    }
}
