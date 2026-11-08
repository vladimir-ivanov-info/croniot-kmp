package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.DeviceFirmware
import com.server.croniot.data.db.daos.DeviceOtaRequest
import com.server.croniot.data.db.daos.FirmwareDao
import com.server.croniot.data.db.daos.FirmwareRelease
import com.server.croniot.data.db.daos.OtaHistoryDao
import com.server.croniot.data.db.daos.OtaUpdate
import com.server.croniot.services.FirmwareImageInfo
import javax.inject.Inject

class FirmwareRepository @Inject constructor(
    private val firmwareDao: FirmwareDao,
    private val otaHistoryDao: OtaHistoryDao,
) {
    fun insertRelease(info: FirmwareImageInfo, image: ByteArray): Long? = firmwareDao.insertRelease(info, image)
    fun listReleases(): List<FirmwareRelease> = firmwareDao.listReleases()
    fun getRelease(id: Long): FirmwareRelease? = firmwareDao.getRelease(id)
    fun findRelease(project: String, version: String): FirmwareRelease? = firmwareDao.findRelease(project, version)
    fun findReleaseByImageHash(hashPrefix: String): FirmwareRelease? = firmwareDao.findReleaseByImageHash(hashPrefix)
    fun getImage(id: Long): ByteArray? = firmwareDao.getImage(id)

    fun upsertRequest(deviceId: Long, releaseId: Long, requestId: String) =
        firmwareDao.upsertRequest(deviceId, releaseId, requestId)
    fun getRequest(deviceId: Long): DeviceOtaRequest? = firmwareDao.getRequest(deviceId)
    fun deleteRequest(deviceId: Long): Boolean = firmwareDao.deleteRequest(deviceId)

    fun insertUpdate(deviceId: Long, releaseId: Long, requestId: String, version: String, fromVersion: String?) =
        otaHistoryDao.insertUpdate(deviceId, releaseId, requestId, version, fromVersion)
    fun finishOpenUpdates(deviceId: Long, status: String, detail: String) =
        otaHistoryDao.finishOpen(deviceId, status, detail)
    fun findUpdate(requestId: String): OtaUpdate? = otaHistoryDao.findByRequestId(requestId)
    fun latestOpenUpdate(deviceId: Long): OtaUpdate? = otaHistoryDao.latestOpen(deviceId)
    fun updateState(id: Long, status: String, progress: Int, detail: String?, finished: Boolean, fromVersion: String?) =
        otaHistoryDao.updateState(id, status, progress, detail, finished, fromVersion)
    fun history(deviceId: Long, limit: Int): List<OtaUpdate> = otaHistoryDao.listForDevice(deviceId, limit)

    fun upsertFirmware(deviceId: Long, firmware: DeviceFirmware) = otaHistoryDao.upsertFirmware(deviceId, firmware)
    fun getFirmware(deviceId: Long): DeviceFirmware? = otaHistoryDao.getFirmware(deviceId)
}
