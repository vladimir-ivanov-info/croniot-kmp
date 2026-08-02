package com.server.croniot.testsupport.fakes

import com.server.croniot.data.db.daos.DeviceTokenDao
import croniot.models.Device
import croniot.models.DeviceToken

class FakeDeviceTokenDao : DeviceTokenDao {

    /** [DeviceToken]s passed to [insert], in order, for tests to assert on directly. */
    val insertedTokens = mutableListOf<DeviceToken>()

    private val deviceByToken = mutableMapOf<String, Device>()
    private val deviceUuidByToken = mutableMapOf<String, String>()
    private val validTokensByDeviceUuid = mutableMapOf<String, MutableSet<String>>()

    /** Seeds a token associated with a full [Device], resolvable via [getDeviceAssociatedWithToken]. */
    fun seed(token: String, device: Device) {
        deviceByToken[token] = device
        deviceUuidByToken[token] = device.uuid
        validTokensByDeviceUuid.getOrPut(device.uuid) { mutableSetOf() }.add(token)
    }

    /** Seeds a token as valid for a device uuid, without a full [Device] record. */
    fun seedValidToken(deviceUuid: String, token: String) {
        deviceUuidByToken[token] = deviceUuid
        validTokensByDeviceUuid.getOrPut(deviceUuid) { mutableSetOf() }.add(token)
    }

    override fun insert(deviceToken: DeviceToken) {
        insertedTokens.add(deviceToken)
    }

    override fun getDeviceAssociatedWithToken(token: String): Device? = deviceByToken[token]

    override fun getDeviceUuidAssociatedWithToken(token: String): String? = deviceUuidByToken[token]

    override fun isTokenCorrect(deviceUuid: String, token: String): Boolean =
        validTokensByDeviceUuid[deviceUuid]?.contains(token) ?: false
}
