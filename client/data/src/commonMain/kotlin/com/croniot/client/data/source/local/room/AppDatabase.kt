package com.croniot.client.data.source.local.room

import androidx.room.Database
import androidx.room.RoomDatabase

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
    abstract fun taskTypeDao(): TaskTypeDao
    abstract fun taskDao(): TaskDao
    abstract fun taskStateInfoDao(): TaskStateInfoDao
    abstract fun sensorDataDao(): SensorDataDao
}
