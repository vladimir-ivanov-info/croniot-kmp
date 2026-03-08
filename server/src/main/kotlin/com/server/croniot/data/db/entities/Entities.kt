package com.server.croniot.data.db.entities
import java.time.ZonedDateTime

/* =========================
   ACCOUNT
   ========================= */

data class AccountEntity(
    val id: Long = 0,
    val uuid: String,
    val nickname: String,
    val email: String,
    val password: String,
)

/* =========================
   DEVICE
   ========================= */

data class DeviceEntity(
    val id: Long = 0,
    val uuid: String,
    val name: String,
    val description: String,
    val iot: Boolean,

    // FK
    val accountId: Long,

    // JSON / key-value si lo tienes así en DB
    //val deviceProperties: Map<String, String> = emptyMap(),
)

/* =========================
   DEVICE TOKEN
   ========================= */

data class DeviceTokenEntity(
    val id: Long = 0,

    // FK
    val deviceId: Long,

    val token: String,
    // val expiryDate: ZonedDateTime?
)

/* =========================
   SENSOR TYPE
   ========================= */

data class SensorTypeEntity(
    val id: Long = 0,
    val uid: Long,
    val name: String,
    val description: String,

    // FK
    val deviceId: Long,
)

/* =========================
   PARAMETER SENSOR
   ========================= */

data class ParameterSensorEntity(
    val id: Long = 0,
    val uid: Long,
    val name: String,
    val type: String,
    val unit: String,
    val description: String,

    // FK
    val sensorTypeId: Long,
)

/* =========================
   PARAMETER SENSOR CONSTRAINT
   ========================= */

data class ParameterSensorConstraintEntity(
    val id: Long = 0,

    // FK
    val parameterId: Long,

    val constraintKey: String,
    val constraintValue: String,
)

/* =========================
   SENSOR DATA
   ========================= */

data class SensorDataEntity(
    val id: Long = 0,

    // FK
    val deviceId: Long,
    val sensorTypeId: Long,

    val value: String,
    val dateTime: ZonedDateTime,
)

/* =========================
   TASK TYPE
   ========================= */

data class TaskTypeEntity(
    val id: Long = 0,
    val uid: Long,
    val name: String,
    val description: String,
    //val realTime: Boolean,

    // FK
    val deviceId: Long,
)

/* =========================
   PARAMETER TASK
   ========================= */

data class ParameterTaskEntity(
    val id: Long = 0,
    val uid: Long,
    val name: String,
    val type: String,
    val unit: String,
    val description: String,

    // FK
    val taskTypeId: Long,
)

/* =========================
   PARAMETER TASK CONSTRAINT
   ========================= */

data class ParameterTaskConstraintEntity(
    val id: Long = 0,

    // FK
    val parameterId: Long,

    val constraintKey: String,
    val constraintValue: String,
)

/* =========================
   TASK
   ========================= */

data class TaskEntity(
    val id: Long = 0,
    val uid: Long,

    // FK
    val taskTypeId: Long,
)

/* =========================
   TASK STATE INFO
   ========================= */

data class TaskStateInfoEntity(
    val id: Long = 0,

    // FK
    val taskId: Long,

    val dateTime: ZonedDateTime,
    val state: String,
    val progress: Double,
    val errorMessage: String?,
)
