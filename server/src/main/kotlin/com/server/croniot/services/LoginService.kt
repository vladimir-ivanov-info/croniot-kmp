package com.server.croniot.services

import com.server.croniot.data.repositories.AccountRepository
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.DeviceTokenRepository
import com.server.croniot.mqtt.MqttController
import croniot.messages.MessageLoginRequest
import croniot.models.Device
import croniot.models.LoginResultDto
import croniot.models.Result
import croniot.models.toDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class LoginService @Inject constructor(
    private val accountRepository: AccountRepository,
    private val deviceRepository: DeviceRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
) {

    fun login(messageLoginRequest: MessageLoginRequest): LoginResultDto {
        var result = LoginResultDto(Result(false, ""), null, null)

// TODO first check email and password
        val accountEmail = messageLoginRequest.email
        val accountPassword = messageLoginRequest.password
        val deviceUuid = messageLoginRequest.deviceUuid
        val deviceToken = messageLoginRequest.deviceToken
        val deviceProperties = messageLoginRequest.deviceProperties

        val account = accountRepository.getAccountEagerSkipTasks(accountEmail, accountPassword) // 1500 ms -> 52 ms
       // account?.devices?.filter { it. }
        // TODO if account null throw new exception "account doesn't exist". Or better check account existence in another method and if exists, pass it to this one
        if (account != null) {
            var device: Device? = null
            deviceToken?.let {
                device = deviceTokenRepository.getDeviceAssociatedWithToken(deviceToken)
            }
            var newToken: String? = null

            if (device == null) {
                val newDevice = Device(uuid = deviceUuid, account = account, deviceProperties = deviceProperties)
                deviceRepository.createDevice(account, newDevice)

                CoroutineScope(Dispatchers.IO).launch {
                    MqttController.listenToNewDevice(newDevice)
                }
                newToken = Global.generateUniqueString(8)
         //       deviceTokenRepository.createDeviceToken(newDevice, newToken)
            }

            // TODO try to authenticate with token first
            val checkToken = false // TODO temporarily we don't need token so I can log in from any device into the same account.
            // if(!checkToken && device != null) {
            result = LoginResultDto(Result(true, ""), account.toDto(), newToken)
            println()
            // }
        }
        return result
    }

    fun loginIot(message: MessageLoginRequest): Result {
        var result = Result(false, "Login failed.")

        val accountEmail = message.email
        val accountPassword = message.password
        val deviceUuid = message.deviceUuid
        val deviceToken = message.deviceToken

        var device: Device? = null
        deviceToken?.let {
            device = deviceTokenRepository.getDeviceAssociatedWithToken(deviceToken) // TODO test for when the device is contained in multiple accounts
        } ?: {
            //TODO try to login with email and password
            //result = Result(false, "Login failed: no token provided.")
        }
// ////////
// TODO try to authenticate with token first
        device?.let {
            val account = accountRepository.getAccountEagerSkipTasks(accountEmail, accountPassword)

            if (account != null) {
                result = Result(true, "Login success")
            }
        } ?: {
            result = Result(false, "Login failed: no device found for given token.")
        }

        return result
    }
}
