package com.server.croniot.data.mappers

import com.server.croniot.data.db.entities.DeviceEntity
import croniot.models.Device
import croniot.models.SensorType
import croniot.models.TaskType

fun Device.toEntity(accountId: Long): DeviceEntity =
    DeviceEntity(
        //id = this.id,
        uuid = this.uuid,
        name = this.name,
        description = this.description,
        iot = this.iot,
        accountId = accountId,
        //deviceProperties = this.deviceProperties,
    )


fun DeviceEntity.toDomain(
    sensorTypes: List<SensorType> = emptyList(),
    taskTypes: List<TaskType> = emptyList()
) = Device(
    uuid = this.uuid,
    name = this.name,
    description = this.description,
    iot = this.iot,
    sensorTypes = sensorTypes,
    taskTypes = taskTypes
)
