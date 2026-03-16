package com.server.croniot.data.db.daos

import com.server.croniot.data.db.entities.AccountEntity
import com.server.croniot.data.db.entities.DeviceEntity
import com.server.croniot.data.db.entities.ParameterSensorConstraintEntity
import com.server.croniot.data.db.entities.ParameterSensorEntity
import com.server.croniot.data.db.entities.ParameterTaskConstraintEntity
import com.server.croniot.data.db.entities.ParameterTaskEntity
import com.server.croniot.data.db.entities.SensorTypeEntity
import com.server.croniot.data.db.entities.TaskTypeEntity
import com.server.croniot.jooq.tables.Account.Companion.ACCOUNT
import com.server.croniot.jooq.tables.Device.Companion.DEVICE
import com.server.croniot.jooq.tables.ParameterSensor.Companion.PARAMETER_SENSOR
import com.server.croniot.jooq.tables.ParameterSensorConstraints.Companion.PARAMETER_SENSOR_CONSTRAINTS
import com.server.croniot.jooq.tables.ParameterTask.Companion.PARAMETER_TASK
import com.server.croniot.jooq.tables.ParameterTaskConstraints.Companion.PARAMETER_TASK_CONSTRAINTS
import com.server.croniot.jooq.tables.SensorType.Companion.SENSOR_TYPE
import com.server.croniot.jooq.tables.TaskType.Companion.TASK_TYPE
import com.server.croniot.jooq.tables.records.DeviceRecord
import com.server.croniot.jooq.tables.records.ParameterSensorConstraintsRecord
import com.server.croniot.jooq.tables.records.ParameterSensorRecord
import com.server.croniot.jooq.tables.records.ParameterTaskConstraintsRecord
import com.server.croniot.jooq.tables.records.ParameterTaskRecord
import com.server.croniot.jooq.tables.records.SensorTypeRecord
import com.server.croniot.jooq.tables.records.TaskTypeRecord
import croniot.models.*
import croniot.models.Device
import org.jooq.DSLContext
import org.jooq.impl.DSL.using
import javax.inject.Inject

class AccountJooqDaoImpl @Inject constructor(
    private val dsl: DSLContext,
) : AccountDao {

    override fun get(email: String): AccountEntity? {
        val rec = dsl
            .selectFrom(ACCOUNT)
            .where(ACCOUNT.EMAIL.eq(email))
            .fetchOne()
            ?: return null

        return AccountEntity(
            id = rec.id!!,
            uuid = rec.uuid!!,
            nickname = rec.nickname!!,
            email = rec.email!!,
            password = rec.password!!,
        )
    }

    override fun insert(account: Account, password: String): Long {
        return dsl.transactionResult { cfg ->
            val tx = using(cfg)

            tx.insertInto(ACCOUNT)
                .set(ACCOUNT.UUID, account.uuid)
                .set(ACCOUNT.NICKNAME, account.nickname)
                .set(ACCOUNT.EMAIL, account.email)
                .set(ACCOUNT.PASSWORD, password)
                .returning(ACCOUNT.ID)
                .fetchOne()!!
                .get(ACCOUNT.ID)!!
        }
    }

    override fun isExistsAccountWithEmail(email: String): Boolean {
        return dsl.fetchExists(
            dsl.selectOne()
                .from(ACCOUNT)
                .where(ACCOUNT.EMAIL.eq(email))
        )
    }

    override fun getAccountEagerSkipTasks(email: String, password: String): Account? {
        return dsl.transactionResult { cfg ->
            val tx = using(cfg)

            val accountEntity = fetchAccountEntity(tx, email, password) ?: return@transactionResult null

            val deviceBundle = fetchDevices(tx, accountEntity.id)
            if (deviceBundle.entities.isEmpty()) {
                return@transactionResult accountEntity.toDomainEmpty()
            }

            val sensorBundle = fetchSensorsGraph(tx, deviceBundle.ids)
            val taskBundle = fetchTaskTypesGraph(tx, deviceBundle.ids)

            assembleAccountDomain(
                accountEntity = accountEntity,
                deviceEntities = deviceBundle.entities,
                sensorGraph = sensorBundle,
                taskGraph = taskBundle,
            )
        }
    }

    /* =========================
       Small helpers (fetch)
       ========================= */

    private data class IdsAndEntities<E>(
        val ids: Set<Long>,
        val entities: List<E>,
    )

    private data class SensorGraph(
        val sensorTypesByDeviceId: Map<Long, List<SensorTypeEntity>>,
        val paramsBySensorTypeId: Map<Long, List<ParameterSensorEntity>>,
        val constraintsByParamId: Map<Long, List<ParameterSensorConstraintEntity>>,
    )

    private data class TaskGraph(
        val taskTypesByDeviceId: Map<Long, List<TaskTypeEntity>>,
        val paramsByTaskTypeId: Map<Long, List<ParameterTaskEntity>>,
        val constraintsByParamId: Map<Long, List<ParameterTaskConstraintEntity>>,
    )

    private fun fetchAccountEntity(
        tx: org.jooq.DSLContext,
        email: String,
        password: String,
    ): AccountEntity? {
        val accRec = tx.selectFrom(ACCOUNT)
            .where(ACCOUNT.EMAIL.eq(email))
            .and(ACCOUNT.PASSWORD.eq(password))
            .fetchOne()
            ?: return null

        val id = accRec.id ?: return null

        return AccountEntity(
            id = id,
            uuid = accRec.uuid ?: "",
            nickname = accRec.nickname ?: "",
            email = accRec.email ?: "",
            password = accRec.password ?: "",
        )
    }

    private fun fetchDevices(
        tx: org.jooq.DSLContext,
        accountId: Long,
    ): IdsAndEntities<DeviceEntity> {
        val recs: List<DeviceRecord> = tx.selectFrom(DEVICE)
            .where(DEVICE.ACCOUNT.eq(accountId))
            .fetch()

        val entities = mutableListOf<DeviceEntity>()
        val ids = mutableSetOf<Long>()

        for (r in recs) {
            val id = r.id ?: continue
            ids.add(id)
            entities.add(
                DeviceEntity(
                    id = id,
                    uuid = r.uuid ?: "",
                    name = r.name ?: "",
                    description = r.description ?: "",
                    iot = r.iot ?: false,
                    accountId = r.account!!,
                )
            )
        }

        return IdsAndEntities(ids = ids, entities = entities)
    }

    private fun fetchSensorsGraph(
        tx: org.jooq.DSLContext,
        deviceIds: Set<Long>,
    ): SensorGraph {
        if (deviceIds.isEmpty()) {
            return SensorGraph(
                sensorTypesByDeviceId = emptyMap(),
                paramsBySensorTypeId = emptyMap(),
                constraintsByParamId = emptyMap(),
            )
        }

        val stRecs: List<SensorTypeRecord> = tx.selectFrom(SENSOR_TYPE)
            .where(SENSOR_TYPE.DEVICE.`in`(deviceIds))
            .fetch()

        val stEntities = mutableListOf<SensorTypeEntity>()
        val stIds = mutableSetOf<Long>()

        for (r in stRecs) {
            val stId = r.id ?: continue
            val devId = r.device ?: continue
            stIds.add(stId)
            stEntities.add(
                SensorTypeEntity(
                    id = stId,
                    uid = r.uid ?: 0L,
                    name = r.name ?: "",
                    description = r.description ?: "",
                    deviceId = devId,
                )
            )
        }

        val pRecs: List<ParameterSensorRecord> =
            if (stIds.isEmpty()) {
                emptyList()
            } else {
                tx.selectFrom(PARAMETER_SENSOR)
                    .where(PARAMETER_SENSOR.SENSOR_TYPE.`in`(stIds))
                    .fetch()
            }

        val pEntities = mutableListOf<ParameterSensorEntity>()
        val pIds = mutableSetOf<Long>()

        for (r in pRecs) {
            val pId = r.id ?: continue
            val stId = r.sensorType ?: continue
            pIds.add(pId)
            pEntities.add(
                ParameterSensorEntity(
                    id = pId,
                    uid = r.uid ?: 0L,
                    name = r.name ?: "",
                    type = r.type ?: "",
                    unit = r.unit ?: "",
                    description = r.description ?: "",
                    sensorTypeId = stId,
                )
            )
        }

        val cRecs: List<ParameterSensorConstraintsRecord> =
            if (pIds.isEmpty()) {
                emptyList()
            } else {
                tx.selectFrom(PARAMETER_SENSOR_CONSTRAINTS)
                    .where(PARAMETER_SENSOR_CONSTRAINTS.PARAMETER_ID.`in`(pIds))
                    .fetch()
            }

        val cEntities = mutableListOf<ParameterSensorConstraintEntity>()
        for (r in cRecs) {
            val pid = r.parameterId ?: continue
            val key = r.constraintKey ?: continue

            cEntities.add(
                ParameterSensorConstraintEntity(
                    id = pid,
                    parameterId = pid,
                    constraintKey = key,
                    constraintValue = r.constraintValue ?: "",
                )
            )
        }

        return SensorGraph(
            sensorTypesByDeviceId = stEntities.groupBy { it.deviceId },
            paramsBySensorTypeId = pEntities.groupBy { it.sensorTypeId },
            constraintsByParamId = cEntities.groupBy { it.parameterId },
        )
    }

    private fun fetchTaskTypesGraph(
        tx: org.jooq.DSLContext,
        deviceIds: Set<Long>,
    ): TaskGraph {
        if (deviceIds.isEmpty()) {
            return TaskGraph(
                taskTypesByDeviceId = emptyMap(),
                paramsByTaskTypeId = emptyMap(),
                constraintsByParamId = emptyMap(),
            )
        }

        val ttRecs: List<TaskTypeRecord> = tx.selectFrom(TASK_TYPE)
            .where(TASK_TYPE.DEVICE.`in`(deviceIds))
            .fetch()

        val ttEntities = mutableListOf<TaskTypeEntity>()
        val ttIds = mutableSetOf<Long>()

        for (r in ttRecs) {
            val ttId = r.id ?: continue
            val devId = r.device ?: continue
            ttIds.add(ttId)
            ttEntities.add(
                TaskTypeEntity(
                    id = ttId,
                    uid = r.uid ?: 0L,
                    name = r.name ?: "",
                    description = r.description ?: "",
                    deviceId = devId,
                )
            )
        }

        val ptRecs: List<ParameterTaskRecord> =
            if (ttIds.isEmpty()) {
                emptyList()
            } else {
                tx.selectFrom(PARAMETER_TASK)
                    .where(PARAMETER_TASK.TASK_TYPE.`in`(ttIds))
                    .fetch()
            }

        val ptEntities = mutableListOf<ParameterTaskEntity>()
        val ptIds = mutableSetOf<Long>()

        for (r in ptRecs) {
            val ptId = r.id ?: continue
            val ttId = r.taskType ?: continue
            ptIds.add(ptId)
            ptEntities.add(
                ParameterTaskEntity(
                    id = ptId,
                    uid = r.uid ?: 0L,
                    name = r.name ?: "",
                    type = r.type ?: "",
                    unit = r.unit ?: "",
                    description = r.description ?: "",
                    taskTypeId = ttId,
                )
            )
        }

        val cRecs: List<ParameterTaskConstraintsRecord> =
            if (ptIds.isEmpty()) {
                emptyList()
            } else {
                tx.selectFrom(PARAMETER_TASK_CONSTRAINTS)
                    .where(PARAMETER_TASK_CONSTRAINTS.PARAMETER_ID.`in`(ptIds))
                    .fetch()
            }

        val cEntities = mutableListOf<ParameterTaskConstraintEntity>()
        for (r in cRecs) {
            val pid = r.parameterId ?: continue
            val key = r.constraintKey ?: continue
            cEntities.add(
                ParameterTaskConstraintEntity(
                    parameterId = pid,
                    constraintKey = key,
                    constraintValue = r.constraintValue ?: "",
                )
            )
        }

        return TaskGraph(
            taskTypesByDeviceId = ttEntities.groupBy { it.deviceId },
            paramsByTaskTypeId = ptEntities.groupBy { it.taskTypeId },
            constraintsByParamId = cEntities.groupBy { it.parameterId },
        )
    }

    /* =========================
       Small helpers (assemble)
       ========================= */

    private fun AccountEntity.toDomainEmpty(): Account =
        Account(
            uuid = uuid,
            nickname = nickname,
            email = email,
            devices = mutableListOf(),
        )

    private fun assembleAccountDomain(
        accountEntity: AccountEntity,
        deviceEntities: List<DeviceEntity>,
        sensorGraph: SensorGraph,
        taskGraph: TaskGraph,
    ): Account {
        val domainDevices = mutableListOf<Device>()

        for (devEntity in deviceEntities) {
            val dId = devEntity.id

            val domainSensorTypes = buildSensorTypesDomain(
                deviceId = dId,
                sensorGraph = sensorGraph,
            )

            val domainTaskTypes = buildTaskTypesDomain(
                deviceId = dId,
                taskGraph = taskGraph,
            )

            val domainDevice = Device(
                uuid = devEntity.uuid,
                name = devEntity.name,
                description = devEntity.description,
                iot = devEntity.iot,
                sensorTypes = domainSensorTypes,
                taskTypes = domainTaskTypes,
            )

            domainDevices.add(domainDevice)
        }

        return Account(
            uuid = accountEntity.uuid,
            nickname = accountEntity.nickname,
            email = accountEntity.email,
            devices = domainDevices,
        )
    }

    private fun buildSensorTypesDomain(
        deviceId: Long,
        sensorGraph: SensorGraph,
    ): MutableList<SensorType> {
        val result = mutableListOf<SensorType>()
        val stEntities = sensorGraph.sensorTypesByDeviceId[deviceId] ?: emptyList()

        for (st in stEntities) {
            val stId = st.id
            val params = mutableListOf<ParameterSensor>()

            val domainSensorType = SensorType(
                uid = st.uid,
                name = st.name,
                description = st.description,
                parameters = params,
            )

            val pEntities = sensorGraph.paramsBySensorTypeId[stId] ?: emptyList()
            for (p in pEntities) {
                val consEntities = sensorGraph.constraintsByParamId[p.id] ?: emptyList()
                val consMap = toConstraintMap(consEntities.map { it.constraintKey to it.constraintValue })

                params.add(
                    ParameterSensor(
                        id = p.id,
                        uid = p.uid,
                        name = p.name,
                        type = p.type,
                        unit = p.unit,
                        description = p.description,
                        constraints = consMap,
                    )
                )
            }

            result.add(domainSensorType)
        }

        return result
    }

    private fun buildTaskTypesDomain(
        deviceId: Long,
        taskGraph: TaskGraph,
    ): MutableList<TaskType> {
        val result = mutableListOf<TaskType>()
        val ttEntities = taskGraph.taskTypesByDeviceId[deviceId] ?: emptyList()

        for (tt in ttEntities) {
            val ttId = tt.id
            val params = mutableListOf<ParameterTask>()

            val domainTaskType = TaskType(
                uid = tt.uid,
                name = tt.name,
                description = tt.description,
                parameters = params,
            )

            val pEntities = taskGraph.paramsByTaskTypeId[ttId] ?: emptyList()
            for (p in pEntities) {
                val consEntities = taskGraph.constraintsByParamId[p.id] ?: emptyList()
                val consMap = toConstraintMap(consEntities.map { it.constraintKey to it.constraintValue })

                params.add(
                    ParameterTask(
                        uid = p.uid,
                        name = p.name,
                        type = p.type,
                        unit = p.unit,
                        description = p.description,
                        constraints = consMap,
                    )
                )
            }

            result.add(domainTaskType)
        }

        return result
    }

    private fun toConstraintMap(pairs: List<Pair<String, String>>): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        for ((k, v) in pairs) {
            if (k.isNotBlank()) map[k] = v
        }
        return map
    }

    override fun getAll(): List<Account> {
        return dsl
            .selectFrom(ACCOUNT)
            .fetch()
            .map { r ->
                Account(
                    uuid = r.uuid!!,
                    nickname = r.nickname!!,
                    email = r.email!!,
                    devices = mutableListOf(),
                )
            }
    }

    override fun isAccountExists(accountEmail: String): Boolean {
        return dsl.fetchExists(
            dsl.selectOne()
                .from(ACCOUNT)
                .where(ACCOUNT.EMAIL.eq(accountEmail))
        )
    }

    override fun getPassword(email: String): String? {
        return dsl
            .select(ACCOUNT.PASSWORD)
            .from(ACCOUNT)
            .where(ACCOUNT.EMAIL.eq(email))
            .fetchOne(ACCOUNT.PASSWORD)
    }

    override fun getAccountId(email: String): Long? {
        return dsl
            .select(ACCOUNT.ID)
            .from(ACCOUNT)
            .where(ACCOUNT.EMAIL.eq(email))
            .fetchOne(ACCOUNT.ID)
    }
}
