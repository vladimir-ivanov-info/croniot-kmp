package com.croniot.server.db

import croniot.models.Account
import croniot.models.Device
import croniot.models.DeviceToken
import com.croniot.server.db.controllers.ControllerDb
import croniot.models.Result
import croniot.messages.MessageRegisterAccount
import croniot.messages.MessageRegisterDevice

object RegisterAccountController {

    fun registerAccount(messageRegisterAccount: MessageRegisterAccount) : Result {

        var result: Result

        try{
            val accountUuid = messageRegisterAccount.accountUuid
            val nickname = messageRegisterAccount.nickname
            val email = messageRegisterAccount.email
            val password = messageRegisterAccount.password

            //TODO check email not empty, etc.

            val account = Account(accountUuid, nickname, email, password, mutableSetOf())

            if(ControllerDb.accountDao.isExistsAccountWithEmail(account.email)){
                result = Result(false, "This email is already used.")
            } else {
                ControllerDb.accountDao.insert(account)
                result = Result(true, "")
            }

        } catch (e: Throwable) {
            result = Result(false, "Could not register account")
        }

        return result
    }

    fun registerDevice(messageRegisterDevice: MessageRegisterDevice) : Result {

        var result: Result

        try{
            val accountEmail = messageRegisterDevice.accountEmail
            val accountPassword = messageRegisterDevice.accountPassword
            val deviceUuid = messageRegisterDevice.deviceUuid
            val deviceName = messageRegisterDevice.deviceName
            val deviceDescription = messageRegisterDevice.deviceDescription

            val account = ControllerDb.accountDao.getAccountEagerSkipTasks(accountEmail, accountPassword)

            if(account != null){
                //TODO check if device exists
                val device = Device(deviceUuid, deviceName, deviceDescription, true, mutableSetOf(), mutableSetOf(), account)
                ControllerDb.deviceDao.insert(device)

                val newToken = Global.generateUniqueString(16)
                val deviceToken = DeviceToken(device, newToken)
                ControllerDb.deviceTokenDao.insert(deviceToken)

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