package com.croniot.client.data.source.remote.mappers

import com.croniot.client.domain.models.Account
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.ParameterSensor
import com.croniot.client.domain.models.ParameterTask
import com.croniot.client.domain.models.SensorData
import com.croniot.client.domain.models.SensorType
import com.croniot.client.domain.models.TaskType
import croniot.models.dto.AccountDto
import croniot.models.dto.DeviceDto
import croniot.models.dto.ParameterSensorDto
import croniot.models.dto.ParameterTaskDto
import croniot.models.dto.SensorDataDto
import croniot.models.dto.SensorTypeDto
import croniot.models.dto.TaskTypeDto

fun AccountDto.toDomain(): Account {
    return Account(
        uuid = this.uuid,
        nickname = this.nickname,
        email = this.email,
        devices = this.devices.map { it.toDomain() }, // .toMutableSet()
    )
}

fun DeviceDto.toDomain(): Device = Device(
    uuid = uuid,
    name = name,
    description = description,
    sensorTypes = sensorTypes.map { it.toDomain() }.toList(),
    taskTypes = taskTypes.map { it.toDomain() }.toList(),
)

fun TaskTypeDto.toDomain(): TaskType {
    return TaskType(
        // id = this.id,
        uid = this.uid,
        name = this.name,
        description = this.description,
        parameters = this.parameters.map { it.toDomain() },
    )
}

fun SensorTypeDto.toDomain(): SensorType {
    return SensorType(
        uid = this.uid,
        name = this.name,
        description = this.description,
        parameters = this.parameters.map { it.toDomain() }.toMutableSet(),
    )
}

fun ParameterTaskDto.toDomain(): ParameterTask {
    return ParameterTask(
        uid = this.uid,
        name = this.name,
        type = this.type,
        unit = this.unit,
        description = this.description,
        constraints = this.constraints,
    )
}

fun ParameterSensorDto.toDomain(): ParameterSensor {
    return ParameterSensor(
        uid = this.uid,
        name = this.name,
        type = this.type,
        unit = this.unit,
        description = this.description,
        constraints = this.constraints,
    )
}

fun SensorDataDto.toDomain(): SensorData = SensorData(
    deviceUuid = deviceUuid,
    sensorTypeUid = sensorTypeUid,
    value = value,
    timeStamp = timestamp,
)
