package com.server.croniot.data.mappers

import croniot.models.Account
import croniot.models.Device
import croniot.models.ParameterSensor
import croniot.models.ParameterTask
import croniot.models.SensorType
import croniot.models.Task
import croniot.models.TaskStateInfo
import croniot.models.TaskType
import croniot.models.dto.AccountDto
import croniot.models.dto.DeviceDto
import croniot.models.dto.ParameterSensorDto
import croniot.models.dto.ParameterTaskDto
import croniot.models.dto.SensorTypeDto
import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoDto
import croniot.models.dto.TaskTypeDto

fun Account.toDto() =
    AccountDto(
        uuid = this.uuid,
        nickname = this.nickname,
        email = this.email,
        devices = this.devices.map { it.toDto() }
    )


fun Device.toDto() =
    DeviceDto(
        uuid = this.uuid,
        name = this.name,
        description = this.description,
        iot = this.iot,
        sensorTypes = this.sensorTypes.map { it.toDto() },
        taskTypes = this.taskTypes.map { it.toDto() },
    )

fun SensorType.toDto() =
    SensorTypeDto(
        uid = this.uid,
        name = this.name,
        description = this.description,
        parameters = this.parameters.map { it.toDto() },
    )



fun ParameterSensor.toDto() =
    ParameterSensorDto(
        uid = this.uid,
        name = this.name,
        type = this.type,
        unit = this.unit,
        description = this.description,
        constraints = this.constraints
    )


fun TaskType.toDto() =
    TaskTypeDto(
        uid = this.uid,
        name = this.name,
        description = this.description,
        parameters = this.parameters.map { it.toDto() }
    )

fun ParameterTask.toDto() =
    ParameterTaskDto(
        uid = this.uid,
        name = this.name,
        type = this.type,
        unit = this.unit,
        description = this.description,
        constraints = this.constraints
    )

fun Task.toDto() = TaskDto(
    uid = this.uid,
    taskTypeUid = this.taskTypeUid,
    parametersValues = this.parametersValues.mapKeys { it.key.uid },
    initialTaskStateInfo = this.mostRecentStateInfo?.toDto(),
)

fun TaskStateInfo.toDto() = TaskStateInfoDto(
    dateTime = this.dateTime,
    state = this.state,
    progress = this.progress,
    errorMessage = this.errorMessage,
)