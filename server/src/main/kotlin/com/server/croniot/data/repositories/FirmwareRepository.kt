package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.DeviceOtaRequest
import com.server.croniot.data.db.daos.FirmwareDao
import com.server.croniot.data.db.daos.FirmwareRelease
import com.server.croniot.services.FirmwareImageInfo
import javax.inject.Inject

class FirmwareRepository @Inject constructor(
    private val firmwareDao: FirmwareDao,
) {
    fun insertRelease(info: FirmwareImageInfo, image: ByteArray): Long? = firmwareDao.insertRelease(info, image)
    fun listReleases(): List<FirmwareRelease> = firmwareDao.listReleases()
    fun getRelease(id: Long): FirmwareRelease? = firmwareDao.getRelease(id)
    fun getImage(id: Long): ByteArray? = firmwareDao.getImage(id)

    fun upsertRequest(deviceId: Long, releaseId: Long, requestId: String) =
        firmwareDao.upsertRequest(deviceId, releaseId, requestId)
    fun getRequest(deviceId: Long): DeviceOtaRequest? = firmwareDao.getRequest(deviceId)
    fun deleteRequest(deviceId: Long): Boolean = firmwareDao.deleteRequest(deviceId)
}
