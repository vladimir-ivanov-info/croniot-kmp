package com.croniot.client.data.source.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.croniot.client.data.source.local.database.daos.AccountDao
import com.croniot.client.data.source.local.database.daos.DeviceDao
import com.croniot.client.data.source.local.database.daos.ParameterSensorDao
import com.croniot.client.data.source.local.database.daos.ParameterTaskDao
import com.croniot.client.data.source.local.database.daos.SensorDataDao
import com.croniot.client.data.source.local.database.daos.SensorTypeDao
import com.croniot.client.data.source.local.database.daos.TaskDao
import com.croniot.client.data.source.local.database.daos.TaskStateInfoDao
import com.croniot.client.data.source.local.database.daos.TaskTypeDao
import com.croniot.client.data.source.local.database.entities.AccountEntity
import com.croniot.client.data.source.local.database.entities.DeviceEntity
import com.croniot.client.data.source.local.database.entities.ParameterSensorEntity
import com.croniot.client.data.source.local.database.entities.ParameterTaskEntity
import com.croniot.client.data.source.local.database.entities.SensorDataEntity
import com.croniot.client.data.source.local.database.entities.SensorTypeEntity
import com.croniot.client.data.source.local.database.entities.TaskEntity
import com.croniot.client.data.source.local.database.entities.TaskStateInfoEntity
import com.croniot.client.data.source.local.database.entities.TaskTypeEntity

@Database(
    entities = [
        AccountEntity::class,
        DeviceEntity::class,
        SensorTypeEntity::class,
        ParameterSensorEntity::class,
        TaskTypeEntity::class,
        ParameterTaskEntity::class,
        TaskEntity::class,
        TaskStateInfoEntity::class,
        SensorDataEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun deviceDao(): DeviceDao
    abstract fun sensorTypeDao(): SensorTypeDao
    abstract fun parameterSensorDao(): ParameterSensorDao
    abstract fun taskTypeDao(): TaskTypeDao
    abstract fun parameterTaskDao(): ParameterTaskDao
    abstract fun taskDao(): TaskDao
    abstract fun taskStateInfoDao(): TaskStateInfoDao
    abstract fun sensorDataDao(): SensorDataDao
}
