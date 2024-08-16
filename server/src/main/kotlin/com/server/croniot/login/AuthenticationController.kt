package com.croniot.server.login

import croniot.models.*
import com.croniot.server.db.controllers.ControllerDb
import croniot.models.dto.AccountDto
import croniot.messages.MessageLogin

object AuthenticationController {

    fun tryLogin(messageLogin: MessageLogin) : AccountDto? {
        var result : AccountDto? = null
//TODO first check email and password
        val accountEmail = messageLogin.accountEmail
        val accountPassword = messageLogin.accountPassword
        val deviceUuid = messageLogin.deviceUuid
        val deviceToken = messageLogin.deviceToken

        val account = ControllerDb.accountDao.getAccount(accountEmail, accountPassword)

        //TODO if account null throw new exception "account doesn't exist". Or better check account existence in another method and if exists, pass it to this one

        var device : Device? = null
        if(deviceToken != null){
            device = getDeviceAssociatedWithToken(deviceToken) //TODO test for when the device is contained in multiple accounts
        }

        if(device == null){
            val newDevice = Device(uuid = deviceUuid, account = account!!)
            ControllerDb.deviceDao.insert(newDevice)
            device = newDevice
            val newToken = Global.generateUniqueString(8)
            ControllerDb.deviceTokenDao.insert(DeviceToken(newDevice, newToken))
        }

        //TODO try to authenticate with token first

        val checkToken = false //TODO temporarily we don't need token so I can log in from any device into the same account.

        if(!checkToken && device != null) {
            if (account != null) {
                result = account.toDto()
                println()
            }
        }
        return result
    }

    fun tryLoginIoT(messageLogin: MessageLogin) : Result {
        var result = Result(false, "Login failed.")

        val accountEmail = messageLogin.accountEmail
        val accountPassword = messageLogin.accountPassword
        val deviceUuid = messageLogin.deviceUuid
        val deviceToken = messageLogin.deviceToken


        var device : Device? = null
        if(deviceToken != null){
            device = getDeviceAssociatedWithToken(deviceToken) //TODO test for when the device is contained in multiple accounts
        }
//////////
       // val device = getDeviceAssociatedWithToken(deviceToken) //TODO test for when the device is contained in multiple accounts
//TODO try to authenticate with token first
        if(device != null) {
            val account = ControllerDb.accountDao.getAccount(accountEmail, accountPassword)

            if (account != null) {
                result = Result(true, "Login success")
            }
        }
        return result
    }


    //TODO CHECK TOKEN
    fun getDeviceAssociatedWithToken(token: String) : Device? {
        val device = ControllerDb.deviceTokenDao.getDeviceAssociatedWithToken(token)

        return device
    }

}