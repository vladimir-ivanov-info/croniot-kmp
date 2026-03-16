package com.server.croniot.data.db.daos

// package com.server.croniot.data.db.daos

import com.server.croniot.jooq.tables.Account.Companion.ACCOUNT
import com.server.croniot.jooq.tables.Device.Companion.DEVICE
import com.server.croniot.jooq.tables.DeviceToken.Companion.DEVICE_TOKEN
import croniot.models.Account
import croniot.models.Device
import croniot.models.DeviceToken
import org.jooq.DSLContext
import org.jooq.impl.DSL.using
import javax.inject.Inject

class DeviceTokenJooqDaoImpl @Inject constructor(
    private val dsl: DSLContext,
) : DeviceTokenDao {

    override fun insert(deviceToken: DeviceToken) {
        // Necesitas FK: device.id (PK interna)
        val deviceId = deviceToken.deviceId
        require(deviceId != 0L) { "DeviceToken.insert requiere device.id (PK interna) no 0" }

        dsl.transaction { cfg ->
            val tx = using(cfg)

            tx.insertInto(DEVICE_TOKEN)
                .set(DEVICE_TOKEN.DEVICE, deviceId) // o DEVICE_ID según tu schema
                .set(DEVICE_TOKEN.TOKEN, deviceToken.token)
                .execute()
        }
    }

    override fun getDeviceAssociatedWithToken(token: String): Device? {
        // Devuelve Device base + Account (para que no haya objetos vacíos)
        // Si prefieres solo DeviceId, te lo adapto.
        val r = dsl
            .select(
                DEVICE.ID,
                DEVICE.UUID,
                DEVICE.NAME,
                DEVICE.DESCRIPTION,
                DEVICE.IOT,
                DEVICE.ACCOUNT,

                ACCOUNT.ID,
                ACCOUNT.UUID,
                ACCOUNT.NICKNAME,
                ACCOUNT.EMAIL,
                ACCOUNT.PASSWORD,
            )
            .from(DEVICE_TOKEN)
            .join(DEVICE).on(DEVICE_TOKEN.DEVICE.eq(DEVICE.ID))
            .join(ACCOUNT).on(DEVICE.ACCOUNT.eq(ACCOUNT.ID))
            .where(DEVICE_TOKEN.TOKEN.eq(token))
            .limit(1)
            .fetchOne()
            ?: return null

        val account = Account(
            // id = r.get(ACCOUNT.ID)!!,
            uuid = r.get(ACCOUNT.UUID) ?: "",
            nickname = r.get(ACCOUNT.NICKNAME) ?: "",
            email = r.get(ACCOUNT.EMAIL) ?: "",
            // password = r.get(ACCOUNT.PASSWORD) ?: "",
            devices = mutableListOf(),
        )

        return Device(
            // id = r.get(DEVICE.ID)!!,
            uuid = r.get(DEVICE.UUID) ?: "",
            name = r.get(DEVICE.NAME) ?: "",
            description = r.get(DEVICE.DESCRIPTION) ?: "",
            iot = r.get(DEVICE.IOT) ?: false,
            sensorTypes = mutableListOf(), // lazy
            taskTypes = mutableListOf(), // lazy
            // account = account,
            // deviceToken = null,            // si quieres, también puedes mapearlo aquí
            // deviceProperties = emptyMap(),
        )
    }

    override fun getDeviceUuidAssociatedWithToken(token: String): String? {
        return dsl
            .select(DEVICE.UUID)
            .from(DEVICE_TOKEN)
            .join(DEVICE).on(DEVICE_TOKEN.DEVICE.eq(DEVICE.ID))
            .where(DEVICE_TOKEN.TOKEN.eq(token))
            .fetchOne(DEVICE.UUID)
    }

    override fun isTokenCorrect(deviceUuid: String, token: String): Boolean {
        return dsl.fetchExists(
            dsl.selectOne()
                .from(DEVICE_TOKEN)
                .join(DEVICE).on(DEVICE_TOKEN.DEVICE.eq(DEVICE.ID))
                .where(DEVICE.UUID.eq(deviceUuid))
                .and(DEVICE_TOKEN.TOKEN.eq(token))
        )
    }
}
