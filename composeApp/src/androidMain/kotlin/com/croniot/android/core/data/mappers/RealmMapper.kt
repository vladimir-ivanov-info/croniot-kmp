package com.croniot.android.core.data.mappers

import com.croniot.android.core.data.entities.DeviceEntity
import com.croniot.android.core.data.entities.KeyValueEntity
import com.croniot.android.core.data.entities.ParameterEntity
import com.croniot.android.core.data.entities.ParameterSensorEntity
import com.croniot.android.core.data.entities.ParameterTaskEntity
import com.croniot.android.core.data.entities.SensorDataEntity
import com.croniot.android.core.data.entities.SensorTypeEntity
import com.croniot.android.core.data.entities.TaskEntity
import com.croniot.android.core.data.entities.TaskStateInfoEntity
import com.croniot.android.core.data.entities.TaskTypeEntity
import croniot.models.dto.DeviceDto
import croniot.models.dto.ParameterDto
import croniot.models.dto.ParameterSensorDto
import croniot.models.dto.ParameterTaskDto
import croniot.models.dto.SensorDataDto
import croniot.models.dto.SensorTypeDto
import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoDto
import croniot.models.dto.TaskTypeDto
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import java.time.ZonedDateTime

fun DeviceDto.toRealmEntity(): DeviceEntity {
    val entity = DeviceEntity()
    entity.uuid = uuid
    entity.name = name
    entity.description = description
    entity.sensors = sensors.map { it.toRealmEntity() }.toRealmList()
    entity.taskTypes = tasks.map { it.toRealmEntity() }.toRealmList()
    return entity
}

fun DeviceEntity.toDto() = DeviceDto(
    uuid = uuid,
    name = name,
    description = description,
    sensors = sensors.map { it.toDto() }.toMutableSet(),
    tasks = taskTypes.map { it.toDto() }.toMutableSet(),
)

fun TaskTypeDto.toRealmEntity(): TaskTypeEntity {
    val entity = TaskTypeEntity()
    entity.uid = uid
    entity.name = name
    entity.description = description
    entity.parameters = parameters.map { it.toRealmEntity() }.toRealmList()
    entity.realTime = realTime
    return entity
}

fun TaskTypeEntity.toDto() = TaskTypeDto(
    uid = uid,
    name = name,
    description = description,
    parameters = parameters.map { it.toDto() }.toMutableSet(),
    realTime = realTime,
)

fun ParameterTaskDto.toRealmEntity(): ParameterTaskEntity {
    val entity = ParameterTaskEntity()
    entity.uid = uid
    entity.name = name
    entity.type = type
    entity.unit = unit
    entity.description = description

    entity.constraints = constraints.map {
        val keyValue = KeyValueEntity()
        keyValue.key = it.key
        keyValue.value = it.value
        keyValue
    }.toRealmList()

    return entity
}

fun ParameterTaskEntity.toDto() = ParameterTaskDto(
    uid = uid,
    name = name,
    type = type,
    unit = unit,
    description = description,
    constraints = constraints.associate { it.key to it.value }.toMutableMap(),
)

fun TaskDto.toRealmEntity(): TaskEntity {
    val entity = TaskEntity()
    entity.uid = uid
    entity.deviceUuid = deviceUuid
    entity.taskTypeUid = taskTypeUid

    entity.parametersValues = parametersValues.map {
        val keyValue = KeyValueEntity()
        keyValue.key = it.key.toString()
        keyValue.value = it.value
        keyValue
    }.toRealmList()

    entity.stateInfos = stateInfos.map { it.toRealmEntity() }.toRealmList()

    return entity
}

fun TaskStateInfoDto.toRealmEntity(): TaskStateInfoEntity {
    val entity = TaskStateInfoEntity()
    entity.deviceUuid = deviceUuid
    entity.taskTypeUid = taskTypeUid
    entity.taskUid = taskUid
    entity.dateTime = 123 // TODO: Reemplazar con conversión real si es necesario
    entity.state = state
    entity.progress = progress
    entity.errorMessage = errorMessage
    return entity
}

fun TaskEntity.toDto() = TaskDto(
    uid = uid,
    deviceUuid = deviceUuid,
    taskTypeUid = taskTypeUid,
    parametersValues = parametersValues.associate { it.key.toLong() to it.value }.toMutableMap(),
    stateInfos = stateInfos.map { it.toDto() }.toMutableSet(),
)

fun TaskStateInfoEntity.toDto() = TaskStateInfoDto(
    deviceUuid = deviceUuid,
    taskTypeUid = taskTypeUid,
    taskUid = taskUid,
    dateTime = ZonedDateTime.now(),
    state = state,
    progress = progress,
    errorMessage = errorMessage,
)

fun ParameterDto.toRealmEntity(): ParameterEntity {
    val entity = ParameterEntity()
    entity.uid = uid
    entity.name = name
    entity.type = type
    entity.unit = unit
    entity.description = description
    entity.constraints = constraints.map {
        val keyValue = KeyValueEntity()
        keyValue.key = it.key
        keyValue.value = it.value
        keyValue
    }.toRealmList()
    return entity
}

fun ParameterSensorEntity.toDto() = ParameterSensorDto(
    uid = uid,
    name = name,
    type = type,
    unit = unit,
    description = description,
    constraints = constraints.associate { it.key to it.value }.toMutableMap(),
)

fun ParameterEntity.toDto() = ParameterDto(
    uid = uid,
    name = name,
    type = type,
    unit = unit,
    description = description,
    constraints = constraints.associate { it.key to it.value }.toMutableMap(),
)

fun SensorDataDto.toRealmEntity(): SensorDataEntity {
    val entity = SensorDataEntity()
    entity.deviceUuid = deviceUuid
    entity.sensorTypeUid = sensorTypeUid
    entity.value = value
    entity.timestamp = timestamp.toInstant().toEpochMilli()
    return entity
}

fun SensorDataEntity.toDto() = SensorDataDto(
    deviceUuid = deviceUuid,
    sensorTypeUid = sensorTypeUid,
    value = value,
    timestamp = ZonedDateTime.now(),
)

fun ParameterSensorDto.toRealmEntity(): ParameterSensorEntity {
    val entity = ParameterSensorEntity()
    entity.uid = uid
    entity.name = name
    entity.type = type
    entity.unit = unit
    entity.description = description
    entity.constraints = constraints.map {
        val keyValue = KeyValueEntity()
        keyValue.key = it.key
        keyValue.value = it.value
        keyValue
    }.toRealmList()
    return entity
}

fun SensorTypeDto.toRealmEntity(): SensorTypeEntity {
    val entity = SensorTypeEntity()
    entity.uid = uid
    entity.name = name
    entity.description = description
    entity.parameters = parameters.mapNotNull { it.toRealmEntity() }.toRealmList()
    return entity
}

fun SensorTypeEntity.toDto() = SensorTypeDto(
    uid = uid,
    name = name,
    description = description,
    parameters = parameters.map { it.toDto() }.toMutableSet(),
)

fun <T> List<T>.toRealmList(): RealmList<T> {
    val realmList = realmListOf<T>()
    realmList.addAll(this)
    return realmList
}
