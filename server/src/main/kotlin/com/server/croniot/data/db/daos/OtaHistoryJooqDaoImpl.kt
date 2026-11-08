package com.server.croniot.data.db.daos

import com.server.croniot.jooq.tables.DeviceFirmware.Companion.DEVICE_FIRMWARE
import com.server.croniot.jooq.tables.DeviceOtaUpdate.Companion.DEVICE_OTA_UPDATE
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import javax.inject.Inject

class OtaHistoryJooqDaoImpl @Inject constructor(
    private val dsl: DSLContext,
) : OtaHistoryDao {

    override fun insertUpdate(
        deviceId: Long,
        releaseId: Long,
        requestId: String,
        version: String,
        fromVersion: String?,
    ) {
        dsl.insertInto(DEVICE_OTA_UPDATE)
            .set(DEVICE_OTA_UPDATE.DEVICE, deviceId)
            .set(DEVICE_OTA_UPDATE.FIRMWARE_RELEASE, releaseId)
            .set(DEVICE_OTA_UPDATE.REQUEST_ID, requestId)
            .set(DEVICE_OTA_UPDATE.VERSION, version)
            .set(DEVICE_OTA_UPDATE.FROM_VERSION, fromVersion)
            .set(DEVICE_OTA_UPDATE.STATUS, "requested")
            .execute()
    }

    override fun finishOpen(deviceId: Long, status: String, detail: String) {
        dsl.update(DEVICE_OTA_UPDATE)
            .set(DEVICE_OTA_UPDATE.STATUS, status)
            .set(DEVICE_OTA_UPDATE.DETAIL, detail)
            .set(DEVICE_OTA_UPDATE.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(DEVICE_OTA_UPDATE.FINISHED_AT, DSL.currentOffsetDateTime())
            .where(DEVICE_OTA_UPDATE.DEVICE.eq(deviceId))
            .and(DEVICE_OTA_UPDATE.FINISHED_AT.isNull)
            .execute()
    }

    override fun findByRequestId(requestId: String): OtaUpdate? {
        return dsl.selectFrom(DEVICE_OTA_UPDATE)
            .where(DEVICE_OTA_UPDATE.REQUEST_ID.eq(requestId))
            .fetchOne { it.toUpdate() }
    }

    override fun latestOpen(deviceId: Long): OtaUpdate? {
        return dsl.selectFrom(DEVICE_OTA_UPDATE)
            .where(DEVICE_OTA_UPDATE.DEVICE.eq(deviceId))
            .and(DEVICE_OTA_UPDATE.FINISHED_AT.isNull)
            .orderBy(DEVICE_OTA_UPDATE.REQUESTED_AT.desc(), DEVICE_OTA_UPDATE.ID.desc())
            .limit(1)
            .fetchOne { it.toUpdate() }
    }

    override fun updateState(
        id: Long,
        status: String,
        progress: Int,
        detail: String?,
        finished: Boolean,
        fromVersion: String?,
    ) {
        dsl.update(DEVICE_OTA_UPDATE)
            .set(DEVICE_OTA_UPDATE.STATUS, status)
            .set(DEVICE_OTA_UPDATE.PROGRESS, progress.toShort())
            .set(DEVICE_OTA_UPDATE.DETAIL, detail)
            .set(DEVICE_OTA_UPDATE.FROM_VERSION, DSL.coalesce(DEVICE_OTA_UPDATE.FROM_VERSION, DSL.`val`(fromVersion)))
            .set(DEVICE_OTA_UPDATE.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(DEVICE_OTA_UPDATE.FINISHED_AT, if (finished) DSL.currentOffsetDateTime() else DSL.`val`(null as java.time.OffsetDateTime?))
            .where(DEVICE_OTA_UPDATE.ID.eq(id))
            .execute()
    }

    override fun listForDevice(deviceId: Long, limit: Int): List<OtaUpdate> {
        return dsl.selectFrom(DEVICE_OTA_UPDATE)
            .where(DEVICE_OTA_UPDATE.DEVICE.eq(deviceId))
            .orderBy(DEVICE_OTA_UPDATE.REQUESTED_AT.desc(), DEVICE_OTA_UPDATE.ID.desc())
            .limit(limit)
            .fetch { it.toUpdate() }
    }

    override fun upsertFirmware(deviceId: Long, firmware: DeviceFirmware) {
        dsl.insertInto(DEVICE_FIRMWARE)
            .set(DEVICE_FIRMWARE.DEVICE, deviceId)
            .set(DEVICE_FIRMWARE.PROJECT, firmware.project)
            .set(DEVICE_FIRMWARE.VERSION, firmware.version)
            .set(DEVICE_FIRMWARE.BUILT_AT, firmware.builtAt)
            .set(DEVICE_FIRMWARE.IDF_VERSION, firmware.idfVersion)
            .set(DEVICE_FIRMWARE.SIZE_BYTES, firmware.sizeBytes)
            .set(DEVICE_FIRMWARE.IMAGE_HASH, firmware.imageHash)
            .set(DEVICE_FIRMWARE.SLOT, firmware.slot)
            .onConflict(DEVICE_FIRMWARE.DEVICE)
            .doUpdate()
            .set(DEVICE_FIRMWARE.PROJECT, firmware.project)
            .set(DEVICE_FIRMWARE.VERSION, firmware.version)
            .set(DEVICE_FIRMWARE.BUILT_AT, firmware.builtAt)
            .set(DEVICE_FIRMWARE.IDF_VERSION, firmware.idfVersion)
            .set(DEVICE_FIRMWARE.SIZE_BYTES, firmware.sizeBytes)
            .set(DEVICE_FIRMWARE.IMAGE_HASH, firmware.imageHash)
            .set(DEVICE_FIRMWARE.SLOT, firmware.slot)
            .set(DEVICE_FIRMWARE.REPORTED_AT, DSL.currentOffsetDateTime())
            .execute()
    }

    override fun getFirmware(deviceId: Long): DeviceFirmware? {
        return dsl.selectFrom(DEVICE_FIRMWARE)
            .where(DEVICE_FIRMWARE.DEVICE.eq(deviceId))
            .fetchOne {
                DeviceFirmware(
                    project = it.get(DEVICE_FIRMWARE.PROJECT)!!,
                    version = it.get(DEVICE_FIRMWARE.VERSION)!!,
                    builtAt = it.get(DEVICE_FIRMWARE.BUILT_AT),
                    idfVersion = it.get(DEVICE_FIRMWARE.IDF_VERSION),
                    sizeBytes = it.get(DEVICE_FIRMWARE.SIZE_BYTES),
                    imageHash = it.get(DEVICE_FIRMWARE.IMAGE_HASH),
                    slot = it.get(DEVICE_FIRMWARE.SLOT),
                    reportedAt = it.get(DEVICE_FIRMWARE.REPORTED_AT)!!,
                )
            }
    }

    private fun Record.toUpdate() = OtaUpdate(
        id = get(DEVICE_OTA_UPDATE.ID)!!,
        deviceId = get(DEVICE_OTA_UPDATE.DEVICE)!!,
        releaseId = get(DEVICE_OTA_UPDATE.FIRMWARE_RELEASE),
        requestId = get(DEVICE_OTA_UPDATE.REQUEST_ID)!!,
        version = get(DEVICE_OTA_UPDATE.VERSION)!!,
        fromVersion = get(DEVICE_OTA_UPDATE.FROM_VERSION),
        status = get(DEVICE_OTA_UPDATE.STATUS)!!,
        progress = get(DEVICE_OTA_UPDATE.PROGRESS)?.toInt() ?: 0,
        detail = get(DEVICE_OTA_UPDATE.DETAIL),
        requestedAt = get(DEVICE_OTA_UPDATE.REQUESTED_AT)!!,
        updatedAt = get(DEVICE_OTA_UPDATE.UPDATED_AT)!!,
        finishedAt = get(DEVICE_OTA_UPDATE.FINISHED_AT),
    )
}
