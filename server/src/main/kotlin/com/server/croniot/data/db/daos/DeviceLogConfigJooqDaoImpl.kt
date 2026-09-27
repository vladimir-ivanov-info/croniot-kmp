package com.server.croniot.data.db.daos

import com.server.croniot.jooq.tables.DeviceLogConfig.Companion.DEVICE_LOG_CONFIG
import org.jooq.DSLContext
import org.jooq.impl.DSL
import javax.inject.Inject

class DeviceLogConfigJooqDaoImpl @Inject constructor(
    private val dsl: DSLContext,
) : DeviceLogConfigDao {

    override fun upsert(deviceId: Long, configJson: String) {
        dsl.insertInto(DEVICE_LOG_CONFIG, DEVICE_LOG_CONFIG.DEVICE, DEVICE_LOG_CONFIG.CONFIG_JSON)
            .values(deviceId, configJson)
            .onConflict(DEVICE_LOG_CONFIG.DEVICE)
            .doUpdate()
            .set(DEVICE_LOG_CONFIG.CONFIG_JSON, configJson)
            .set(DEVICE_LOG_CONFIG.UPDATED_AT, DSL.currentOffsetDateTime())
            .execute()
    }

    override fun get(deviceId: Long): String? {
        return dsl.select(DEVICE_LOG_CONFIG.CONFIG_JSON)
            .from(DEVICE_LOG_CONFIG)
            .where(DEVICE_LOG_CONFIG.DEVICE.eq(deviceId))
            .fetchOne(DEVICE_LOG_CONFIG.CONFIG_JSON)
    }
}
