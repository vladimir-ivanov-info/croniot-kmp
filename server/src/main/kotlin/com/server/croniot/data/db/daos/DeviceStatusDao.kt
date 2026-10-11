package com.server.croniot.data.db.daos

import com.server.croniot.jooq.tables.DeviceStatus.Companion.DEVICE_STATUS
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.time.OffsetDateTime
import javax.inject.Inject

data class DeviceStatusRow(val online: Boolean, val bootId: Long?, val updatedAt: OffsetDateTime)

class DeviceStatusDao @Inject constructor(
    private val dsl: DSLContext,
) {
    // Insert or overwrite. A null bootId (an "offline" message) keeps the
    // boot the device last announced.
    fun upsert(deviceId: Long, online: Boolean, bootId: Long?) {
        dsl.insertInto(DEVICE_STATUS)
            .set(DEVICE_STATUS.DEVICE, deviceId)
            .set(DEVICE_STATUS.ONLINE, online)
            .set(DEVICE_STATUS.BOOT_ID, bootId)
            .set(DEVICE_STATUS.UPDATED_AT, DSL.currentOffsetDateTime())
            .onConflict(DEVICE_STATUS.DEVICE)
            .doUpdate()
            .set(DEVICE_STATUS.ONLINE, online)
            .set(DEVICE_STATUS.BOOT_ID, DSL.coalesce(DSL.`val`(bootId), DEVICE_STATUS.BOOT_ID))
            .set(DEVICE_STATUS.UPDATED_AT, DSL.currentOffsetDateTime())
            .execute()
    }

    fun get(deviceId: Long): DeviceStatusRow? =
        dsl.selectFrom(DEVICE_STATUS)
            .where(DEVICE_STATUS.DEVICE.eq(deviceId))
            .fetchOne { DeviceStatusRow(it.online ?: false, it.bootId, it.updatedAt ?: OffsetDateTime.now()) }
}
