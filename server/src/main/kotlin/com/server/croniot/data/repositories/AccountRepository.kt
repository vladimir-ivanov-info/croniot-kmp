package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.AccountDao
import com.server.croniot.data.db.daos.DeviceDao
import com.server.croniot.data.db.daos.SensorTypeDao
import com.server.croniot.data.db.daos.TaskTypeDao
import com.server.croniot.data.mappers.toDomain
import croniot.models.Account
import croniot.models.Device
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val deviceDao: DeviceDao,
    private val sensorTypeDao: SensorTypeDao,
    private val taskTypeDao: TaskTypeDao

) {

    fun getAccount(email: String) : Account? {
        //return accountDao.get(email)

        var result : Account? = null

        val accountEntity = accountDao.get(email) //TODO avoid getting password in future

        if(accountEntity != null){
            val devicesEntity = deviceDao.getDevices(accountEntity.id)

//
            val deviceIds = devicesEntity.map{ it.id }
            val sensorTypesMap = sensorTypeDao.getByDeviceIds(deviceIds)
            val taskTypesMap = taskTypeDao.getByDeviceIds(deviceIds)
        //TODO taskTypesMap



                //println(sensorTypes.size)

            val devices = mutableListOf<Device>()

            for(deviceEntity in devicesEntity){

                val deviceEntityId = deviceEntity.id
                val sensorTypes = sensorTypesMap[deviceEntityId] ?: emptyList()
                val taskTypes = taskTypesMap[deviceEntityId] ?: emptyList()

                val deviceDomain = deviceEntity.toDomain(
                    sensorTypes = sensorTypes,
                    taskTypes = taskTypes
                )
                devices.add(deviceDomain)
            }


           // val devicesBootstrap = devices.map { it.toBootstrap() }

            /*result = AccountBootstrap(
                uuid = accountEntity.uuid,
                nickname = accountEntity.nickname,
                email = accountEntity.email,
                devices = devices
            )
            */
            result = Account(
                uuid = accountEntity.uuid,
                nickname = accountEntity.nickname,
                email = accountEntity.email,
                devices = devices
            )
        }

        return result
    }



    fun getAccountId(email: String) : Long? {
        return accountDao.getAccountId(email)
    }

    fun isEmailAvailable(email: String): Boolean {
        val exists = accountDao.isExistsAccountWithEmail(email)

        // Add domain-specific logic (if any)
        return !exists // Email is available if it doesn't exist
    }

    fun createAccount(accountUuid: String, nickname: String, email: String, password: String): Long {
        // Perform domain-specific logic (e.g., hashing passwords)
        // val hashedPassword = hashPassword(plainPassword)
        // val account = Account(email = email, password = hashedPassword)
        val account = Account(accountUuid, nickname, email, /*password,*/ mutableListOf())
        return accountDao.insert(account, password)
    }

    fun getAccountEagerSkipTasks(accountEmail: String, accountPassword: String): Account? {
        return accountDao.getAccountEagerSkipTasks(accountEmail, accountPassword)
    }

    fun isAccountExists(accountEmail: String) : Boolean {
        return accountDao.isAccountExists(accountEmail)
    }

    fun getPassword(email: String) : String? {
        return accountDao.getPassword(email)
    }

    // TODO see if this function should be here or in DeviceRepository
    // TODO assicated with /api/account_info, which will probably be deleted
    fun getAccountOfDevice(device: Device): List<Account> {
        // TODO
        return mutableListOf()
    }
}
