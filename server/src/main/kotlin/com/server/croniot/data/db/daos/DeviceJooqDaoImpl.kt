package com.server.croniot.data.db.daos

import croniot.models.Account
import croniot.models.Device
import org.jooq.DSLContext
import org.jooq.impl.DSL.using
import javax.inject.Inject

import com.server.croniot.jooq.tables.Device.Companion.DEVICE
import com.server.croniot.jooq.tables.Account.Companion.ACCOUNT
import com.server.croniot.data.db.entities.AccountEntity
import com.server.croniot.data.db.entities.DeviceEntity

class DeviceJooqDaoImpl @Inject constructor(
    private val dsl: DSLContext,
) : DeviceDao {

    override fun getDevices(accountId: Long): List<DeviceEntity> {
        /*return dsl
            .selectFrom(DEVICE)
            .where(DEVICE.ACCOUNT.eq(accountId))
            .fetchInto(DeviceEntity::class.java)*/

        return dsl
            .selectFrom(DEVICE)
            .where(DEVICE.ACCOUNT.eq(accountId))
            .fetch { rec ->
                DeviceEntity(
                    id = rec.id!!,
                    uuid = rec.uuid!!,
                    name = rec.name ?: "",
                    description = rec.description ?: "",
                    iot = rec.iot ?: false,
                    accountId = rec.account!!,
                    //deviceProperties = emptyMap() // o mapear si lo tienes en DB
                )
            }
    }

    /*override fun insert(device: DeviceEntity) {
        require(device.accountId != 0L) {
            "insert(account, device) requiere account.id (PK interna). " +
                    "Si solo tienes account.uuid/email, primero resuélvelo."
        }

        dsl.transaction { cfg ->
            val tx = using(cfg)

            tx.insertInto(DEVICE)
                .set(DEVICE.UUID, device.uuid)
                .set(DEVICE.NAME, device.name)
                .set(DEVICE.DESCRIPTION, device.description)
                .set(DEVICE.IOT, device.iot)
                .set(DEVICE.ACCOUNT, device.accountId)
                .execute()

            // Children (sensorTypes/taskTypes) are inserted via SensorTypeDao/TaskTypeDao.
        }
    }*/

    override fun insert(device: DeviceEntity): Long {
        require(device.accountId != 0L) {
            "insert(device) requiere device.accountId (PK interna). " +
                    "Si solo tienes account.uuid/email, primero resuélvelo."
        }

        return dsl.transactionResult { cfg ->
            val tx = using(cfg)

            val rec = tx.insertInto(DEVICE)
                .set(DEVICE.UUID, device.uuid)
                .set(DEVICE.NAME, device.name)
                .set(DEVICE.DESCRIPTION, device.description)
                .set(DEVICE.IOT, device.iot)
                .set(DEVICE.ACCOUNT, device.accountId)
                .returning(DEVICE.ID)
                .fetchOne() ?: error("Insert DEVICE no devolvió registro (returning).")

            rec.get(DEVICE.ID) ?: error("Insert DEVICE no devolvió DEVICE.ID.")
        }
    }

    override fun upsert(device: DeviceEntity): Long {
        require(device.accountId != 0L)

        return dsl.transactionResult { cfg ->
            val tx = using(cfg)

            val rec = tx.insertInto(DEVICE)
                .set(DEVICE.UUID, device.uuid)
                .set(DEVICE.NAME, device.name)
                .set(DEVICE.DESCRIPTION, device.description)
                .set(DEVICE.IOT, device.iot)
                .set(DEVICE.ACCOUNT, device.accountId)
                .onConflict(DEVICE.UUID)              // conflicto por UNIQUE(uuid)
                .doUpdate()
                .set(DEVICE.NAME, device.name)
                .set(DEVICE.DESCRIPTION, device.description)
                .set(DEVICE.IOT, device.iot)
                .set(DEVICE.ACCOUNT, device.accountId) // si quieres permitir moverlo de cuenta
                .returning(DEVICE.ID)
                .fetchOne() ?: error("Upsert DEVICE no devolvió registro.")

            rec.get(DEVICE.ID) ?: error("Upsert DEVICE no devolvió DEVICE.ID.")
        }
    }


    override fun getAll(): List<Device> {
        // Join with ACCOUNT so Device has a non-null accountId.
        return dsl
            .select(
                DEVICE.ID,
                DEVICE.UUID,
                DEVICE.NAME,
                DEVICE.DESCRIPTION,
                DEVICE.IOT,

                ACCOUNT.ID,
                ACCOUNT.UUID,
                ACCOUNT.NICKNAME,
                ACCOUNT.EMAIL,
                ACCOUNT.PASSWORD,
            )
            .from(DEVICE)
            .join(ACCOUNT).on(DEVICE.ACCOUNT.eq(ACCOUNT.ID))
            .fetch()
            .map { r ->
                val account = Account(
                    //id = r.get(ACCOUNT.ID)!!,
                    uuid = r.get(ACCOUNT.UUID) ?: "",
                    nickname = r.get(ACCOUNT.NICKNAME) ?: "",
                    email = r.get(ACCOUNT.EMAIL) ?: "",
                    //password = r.get(ACCOUNT.PASSWORD) ?: "",
                    devices = mutableListOf(), // evitamos ciclos
                )

                Device(
                    //id = r.get(DEVICE.ID)!!,
                    uuid = r.get(DEVICE.UUID) ?: "",
                    name = r.get(DEVICE.NAME) ?: "",
                    description = r.get(DEVICE.DESCRIPTION) ?: "",
                    iot = r.get(DEVICE.IOT) ?: false,
                    sensorTypes = mutableListOf(), // lazy aquí
                    taskTypes = mutableListOf(),   // lazy aquí
                    //account = account,
                    //deviceToken = null,
                    //deviceProperties = emptyMap(), // si lo tienes en DB, mapea aquí
                )
            }
    }

    override fun getByUuid(deviceUuid: String): Device? {
        // limit(1) guards against duplicate UUIDs in the database.
        val r = dsl
            .select(
                DEVICE.ID,
                DEVICE.UUID,
                DEVICE.NAME,
                DEVICE.DESCRIPTION,
                DEVICE.IOT,

                ACCOUNT.ID,
                ACCOUNT.UUID,
                ACCOUNT.NICKNAME,
                ACCOUNT.EMAIL,
                ACCOUNT.PASSWORD,
            )
            .from(DEVICE)
            .join(ACCOUNT).on(DEVICE.ACCOUNT.eq(ACCOUNT.ID))
            .where(DEVICE.UUID.eq(deviceUuid))
            .limit(1)
            .fetchOne()
            ?: return null

        val account = Account(
            //id = r.get(ACCOUNT.ID)!!,
            uuid = r.get(ACCOUNT.UUID) ?: "",
            nickname = r.get(ACCOUNT.NICKNAME) ?: "",
            email = r.get(ACCOUNT.EMAIL) ?: "",
            //password = r.get(ACCOUNT.PASSWORD) ?: "",
            devices = mutableListOf(),
        )

        return Device(
            //id = r.get(DEVICE.ID)!!,
            uuid = r.get(DEVICE.UUID) ?: "",
            name = r.get(DEVICE.NAME) ?: "",
            description = r.get(DEVICE.DESCRIPTION) ?: "",
            iot = r.get(DEVICE.IOT) ?: false,
            sensorTypes = mutableListOf(), // lazy
            taskTypes = mutableListOf(),   // lazy
            //account = account,
            //deviceToken = null,
            //deviceProperties = emptyMap(),
        )
    }

    override fun getLazy(deviceUuid: String): Device? {
        // Lazy pero coherente: traemos id/uuid + accountId
        val r = dsl
            .select(
                DEVICE.ID,
                DEVICE.UUID,
                DEVICE.ACCOUNT
            )
            .from(DEVICE)
            .where(DEVICE.UUID.eq(deviceUuid))
            .limit(1)
            .fetchOne()
            ?: return null

        val accountId = r.get(DEVICE.ACCOUNT) ?: return null

        // Account “lite” (para no dejarlo vacío/inválido)
        val accountLite = Account(
            //id = accountId,
            uuid = "",
            nickname = "",
            email = "",
            //password = "",
            devices = mutableListOf(),
        )

        return Device(
            // id = r.get(DEVICE.ID)!!,
            uuid = r.get(DEVICE.UUID) ?: "",
            name = "",
            description = "",
            iot = false,
            sensorTypes = mutableListOf(),
            taskTypes = mutableListOf(),
            //account = accountLite,
            //deviceToken = null,
            //deviceProperties = emptyMap(),
        )
    }

    override fun getDeviceId(deviceUuid: String): Long? {
        return dsl
            .select(DEVICE.ID)
            .from(DEVICE)
            .where(DEVICE.UUID.eq(deviceUuid))
            .fetchOne(DEVICE.ID)
    }

    override fun isDeviceExists(deviceUuid: String): Boolean {
        return dsl
            .selectCount()
            .from(DEVICE)
            .where(DEVICE.UUID.eq(deviceUuid))
            .fetchOne(0, Int::class.java)!! > 0
    }

}