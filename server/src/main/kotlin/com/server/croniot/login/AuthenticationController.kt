package com.croniot.server.login

import Global
import MqttController
import com.croniot.server.db.controllers.ControllerDb
import croniot.messages.MessageLoginRequest
import croniot.models.*


object AuthenticationController {

    fun tryLogin(messageLoginRequest: MessageLoginRequest) : LoginResult {

        var result = LoginResult(Result(false, ""), null, null)

//TODO first check email and password
        val accountEmail = messageLoginRequest.email
        val accountPassword = messageLoginRequest.password
        val deviceUuid = messageLoginRequest.deviceUuid
        val deviceToken = messageLoginRequest.deviceToken
        val deviceProperties = messageLoginRequest.deviceProperties

        val startMillis = System.currentTimeMillis()

        val account = ControllerDb.accountDao.getAccountEagerSkipTasks(accountEmail, accountPassword) //1500 ms -> 52 ms
        val endMillis = System.currentTimeMillis()
        val time = endMillis - startMillis
       // println("$time")

       // val stats: Statistics = ControllerDb.sessionFactory.statistics
       // println("Second-level cache hit count: " + stats.secondLevelCacheHitCount)
       // println("Second-level cache miss count: " + stats.secondLevelCacheMissCount)
       // println()
        //TODO if account null throw new exception "account doesn't exist". Or better check account existence in another method and if exists, pass it to this one
        if(account != null){
            var device : Device? = null
            deviceToken?.let {
                device = ControllerDb.deviceTokenDao.getDeviceAssociatedWithToken(deviceToken)
            }
            var newToken : String? = null

            if(device == null){
                val newDevice = Device(uuid = deviceUuid, account = account, deviceProperties = deviceProperties)
                ControllerDb.deviceDao.insert(newDevice)

                MqttController.listenToNewDevice(newDevice)

                newToken = Global.generateUniqueString(8)
                ControllerDb.deviceTokenDao.insert(DeviceToken(newDevice, newToken))
            }

            //TODO try to authenticate with token first
            val checkToken = false //TODO temporarily we don't need token so I can log in from any device into the same account.
            // if(!checkToken && device != null) {
                result = LoginResult(Result(true, ""), account.toDto(), newToken)
                println()
           // }
        }
        return result
    }

    fun tryLoginIoT(messageLoginRequest: MessageLoginRequest) : Result {
        var result = Result(false, "Login failed.")

        val accountEmail = messageLoginRequest.email
        val accountPassword = messageLoginRequest.password
        val deviceUuid = messageLoginRequest.deviceUuid
        val deviceToken = messageLoginRequest.deviceToken

        var device : Device? = null
        deviceToken?.let {
            device = getDeviceAssociatedWithToken(deviceToken) //TODO test for when the device is contained in multiple accounts
        }
//////////
       // val device = getDeviceAssociatedWithToken(deviceToken) //TODO test for when the device is contained in multiple accounts
//TODO try to authenticate with token first

        device?.let {
            val account = ControllerDb.accountDao.getAccountEagerSkipTasks(accountEmail, accountPassword)

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