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
    private val deviceTokenRepository: DeviceTokenRepository
) {

    fun getLazy(deviceUuid: String) : Device? {
        return deviceRepository.getLazy(deviceUuid)
    }

    fun getByUuid(deviceUuid: String) : Device? {
        return deviceRepository.getByUuid(deviceUuid)
    }

    fun getAll() : List<Device> {
        return deviceRepository.getAll()
    }

    fun registerDevice(messageRegisterDevice: MessageRegisterDevice): Result {

        var result: Result

        try{
            val accountEmail = messageRegisterDevice.accountEmail
            val accountPassword = messageRegisterDevice.accountPassword
            val deviceUuid = messageRegisterDevice.deviceUuid
            val deviceName = messageRegisterDevice.deviceName
            val deviceDescription = messageRegisterDevice.deviceDescription

            val account = accountRepository.getAccountEagerSkipTasks(accountEmail, accountPassword)

            if(account != null){
                //TODO check if device exists
                val device = Device(deviceUuid, deviceName, deviceDescription, true, mutableSetOf(), mutableSetOf(), account)
                deviceRepository.createDevice(device)

                val newToken = Global.generateUniqueString(16)
                deviceTokenRepository.createDeviceToken(device, newToken)

                result = Result(true, newToken)
            } else {
                result = Result(false, "Account for $accountEmail doesn't exist.")
            }
        } catch (e: Throwable) {
            result = Result(false, "Could not register device.")
        }

        return result
    }

 }