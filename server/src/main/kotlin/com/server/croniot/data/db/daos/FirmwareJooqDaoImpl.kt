package com.server.croniot.data.db.daos

import com.server.croniot.jooq.tables.DeviceOtaRequest.Companion.DEVICE_OTA_REQUEST
import com.server.croniot.jooq.tables.FirmwareRelease.Companion.FIRMWARE_RELEASE
import com.server.croniot.services.FirmwareImageInfo
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import javax.inject.Inject

class FirmwareJooqDaoImpl @Inject constructor(
    private val dsl: DSLContext,
) : FirmwareDao {

    override fun insertRelease(info: FirmwareImageInfo, image: ByteArray): Long? {
        return dsl.insertInto(FIRMWARE_RELEASE)
            .set(FIRMWARE_RELEASE.PROJECT, info.project)
            .set(FIRMWARE_RELEASE.VERSION, info.version)
            .set(FIRMWARE_RELEASE.SIZE_BYTES, info.sizeBytes)
            .set(FIRMWARE_RELEASE.SHA256, info.sha256)
            .set(FIRMWARE_RELEASE.IMAGE, image)
            .onConflictDoNothing()
            .returning(FIRMWARE_RELEASE.ID)
            .fetchOne()
            ?.get(FIRMWARE_RELEASE.ID)
    }

    // Never selects the image column: listing must not pull megabytes per row.
    override fun listReleases(): List<FirmwareRelease> {
        return dsl.select(*infoFields())
            .from(FIRMWARE_RELEASE)
            .orderBy(FIRMWARE_RELEASE.CREATED_AT.desc(), FIRMWARE_RELEASE.ID.desc())
            .fetch { it.toRelease() }
    }

    override fun getRelease(id: Long): FirmwareRelease? {
        return dsl.select(*infoFields())
            .from(FIRMWARE_RELEASE)
            .where(FIRMWARE_RELEASE.ID.eq(id))
            .fetchOne { it.toRelease() }
    }

    override fun getImage(id: Long): ByteArray? {
        return dsl.select(FIRMWARE_RELEASE.IMAGE)
            .from(FIRMWARE_RELEASE)
            .where(FIRMWARE_RELEASE.ID.eq(id))
            .fetchOne(FIRMWARE_RELEASE.IMAGE)
    }

    override fun upsertRequest(deviceId: Long, releaseId: Long, requestId: String) {
        dsl.insertInto(DEVICE_OTA_REQUEST)
            .set(DEVICE_OTA_REQUEST.DEVICE, deviceId)
            .set(DEVICE_OTA_REQUEST.FIRMWARE_RELEASE, releaseId)
            .set(DEVICE_OTA_REQUEST.REQUEST_ID, requestId)
            .onConflict(DEVICE_OTA_REQUEST.DEVICE)
            .doUpdate()
            .set(DEVICE_OTA_REQUEST.FIRMWARE_RELEASE, releaseId)
            .set(DEVICE_OTA_REQUEST.REQUEST_ID, requestId)
            .set(DEVICE_OTA_REQUEST.REQUESTED_AT, DSL.currentOffsetDateTime())
            .execute()
    }

    override fun getRequest(deviceId: Long): DeviceOtaRequest? {
        return dsl.selectFrom(DEVICE_OTA_REQUEST)
            .where(DEVICE_OTA_REQUEST.DEVICE.eq(deviceId))
            .fetchOne {
                DeviceOtaRequest(
                    deviceId = it.get(DEVICE_OTA_REQUEST.DEVICE)!!,
                    releaseId = it.get(DEVICE_OTA_REQUEST.FIRMWARE_RELEASE)!!,
                    requestId = it.get(DEVICE_OTA_REQUEST.REQUEST_ID)!!,
                    requestedAt = it.get(DEVICE_OTA_REQUEST.REQUESTED_AT)!!,
                )
            }
    }

    override fun deleteRequest(deviceId: Long): Boolean {
        return dsl.deleteFrom(DEVICE_OTA_REQUEST)
            .where(DEVICE_OTA_REQUEST.DEVICE.eq(deviceId))
            .execute() > 0
    }

    private fun infoFields() = arrayOf(
        FIRMWARE_RELEASE.ID,
        FIRMWARE_RELEASE.PROJECT,
        FIRMWARE_RELEASE.VERSION,
        FIRMWARE_RELEASE.SIZE_BYTES,
        FIRMWARE_RELEASE.SHA256,
        FIRMWARE_RELEASE.CREATED_AT,
    )

    private fun Record.toRelease() = FirmwareRelease(
        id = get(FIRMWARE_RELEASE.ID)!!,
        project = get(FIRMWARE_RELEASE.PROJECT)!!,
        version = get(FIRMWARE_RELEASE.VERSION)!!,
        sizeBytes = get(FIRMWARE_RELEASE.SIZE_BYTES)!!,
        sha256 = get(FIRMWARE_RELEASE.SHA256)!!,
        createdAt = get(FIRMWARE_RELEASE.CREATED_AT)!!,
    )
}
