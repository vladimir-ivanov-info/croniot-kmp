package com.croniot.android.core.data.mappers

import com.croniot.android.domain.model.Account
import com.croniot.android.domain.model.Device
import com.croniot.android.domain.model.ParameterSensor
import com.croniot.android.domain.model.ParameterTask
import com.croniot.android.domain.model.SensorType
import com.croniot.android.domain.model.TaskType
import croniot.models.dto.AccountDto
import croniot.models.dto.DeviceDto
import croniot.models.dto.ParameterSensorDto
import croniot.models.dto.ParameterTaskDto
import croniot.models.dto.SensorTypeDto
import croniot.models.dto.TaskTypeDto

fun AccountDto.toAndroidModel(): Account {
    return Account(
        uuid = this.uuid,
        nickname = this.nickname,
        email = this.email,
        devices = this.devices.map { it.toAndroidModel() }.toMutableSet(),
    )
}

fun DeviceDto.toAndroidModel(): Device {
    return Device(
        uuid = this.uuid,
        name = this.name,
        description = this.description,
        sensors = this.sensors.map { it.toAndroidModel() }.toMutableSet(),
        taskTypes = this.tasks.map { it.toAndroidModel() }.toMutableSet(),
    )
}

fun TaskTypeDto.toAndroidModel(): TaskType {
    return TaskType(
        id = this.id,
        uid = this.uid,
        name = this.name,
        description = this.description,
        parameters = this.parameters.map { it.toAndroidModel() }.toMutableSet(),
        realTime = this.realTime,
    )
}

fun SensorTypeDto.toAndroidModel(): SensorType {
    return SensorType(
        uid = this.uid,
        name = this.name,
        description = this.description,
        parameters = this.parameters.map { it.toAndroidModel() }.toMutableSet(),
    )
}

fun ParameterTaskDto.toAndroidModel(): ParameterTask {
    return ParameterTask(
        uid = this.uid,
        name = this.name,
        type = this.type,
        unit = this.unit,
        description = this.description,
        constraints = this.constraints.toMutableMap(), // TODO not actually mutable map, convert to map
    )
}

fun ParameterSensorDto.toAndroidModel(): ParameterSensor {
    return ParameterSensor(
        uid = this.uid,
        name = this.name,
        type = this.type,
        unit = this.unit,
        description = this.description,
        constraints = this.constraints.toMutableMap(), // TODO not actually mutable map, convert to map
    )
}
