package com.server.croniot.livelog

import com.server.croniot.jooq.tables.Account.Companion.ACCOUNT
import com.server.croniot.jooq.tables.Device.Companion.DEVICE
import org.jooq.DSLContext
import javax.inject.Inject

data class DeviceContext(val device: LogDeviceRef, val owner: LogOwnerRef)

// The "whose is this?" part of a live log entry: device name plus its
// owning account, in one join. Called once per ingested batch, never per
// record.
class DeviceContextLookup @Inject constructor(
    private val dsl: DSLContext,
) {
    fun byDeviceId(deviceId: Long): DeviceContext? {
        return dsl.select(DEVICE.UUID, DEVICE.NAME, ACCOUNT.UUID, ACCOUNT.NICKNAME, ACCOUNT.EMAIL)
            .from(DEVICE)
            .join(ACCOUNT).on(ACCOUNT.ID.eq(DEVICE.ACCOUNT))
            .where(DEVICE.ID.eq(deviceId))
            .fetchOne { r ->
                DeviceContext(
                    device = LogDeviceRef(uuid = r.value1() ?: "", name = r.value2()),
                    owner = LogOwnerRef(uuid = r.value3() ?: "", nickname = r.value4(), email = r.value5() ?: ""),
                )
            }
    }
}
