package com.server.croniot.services

import com.server.croniot.data.repositories.AccountRepository
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.DeviceTokenRepository
import croniot.messages.MessageRegisterDevice
import croniot.models.Device
import croniot.models.Result
import javax.inject.Inject

class DeviceService @Inject constructor(
    private val accountRepository: AccountRepository,
    private val deviceRepository: DeviceRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
) {

    fun getId(uuid: String): Long? {
        return deviceRepository.getId(uuid)
    }

    fun getLazy(deviceUuid: String): Device? {
        return deviceRepository.getLazy(deviceUuid)
    }

    fun getByUuid(deviceUuid: String): Device? {
        return deviceRepository.getByUuid(deviceUuid)
    }

    fun getAll(): List<Device> {
        return deviceRepository.getAll()
    }

    fun registerDevice(messageRegisterDevice: MessageRegisterDevice): Result {
        try {
            val accountEmail = messageRegisterDevice.accountEmail
            val accountPassword = messageRegisterDevice.accountPassword
            val deviceUuid = messageRegisterDevice.deviceUuid
            val deviceName = messageRegisterDevice.deviceName
            val deviceDescription = messageRegisterDevice.deviceDescription

            val accountExists = accountRepository.isAccountExists(accountEmail)
            //TODO check password

            if (!accountExists) {
                return Result(false, "Account for $accountEmail doesn't exist.")
            }

            val device = Device(deviceUuid, deviceName, deviceDescription, true)
            val accountId = accountRepository.getAccountId(accountEmail)
                ?: return Result(false, "Account for $accountEmail doesn't exist.")

            val deviceId = deviceRepository.createDevice(device, accountId)
            val newToken = Global.generateUniqueString(16)
            deviceTokenRepository.createDeviceToken(deviceId, newToken)

            return Result(true, newToken)
        } catch (e: Throwable) {
            return Result(true, "Could not register device, probably it already exists.")
        }
    }
}
