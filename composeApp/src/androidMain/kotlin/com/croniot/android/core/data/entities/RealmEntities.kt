package com.croniot.android.core.data.entities
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.serialization.Required
import java.util.UUID

class AccountEntity : RealmObject {
    @PrimaryKey
    var uuid: String = ""
    var nickname: String = ""
    var email: String = ""
    var devices: RealmList<DeviceEntity> = realmListOf()
}

class DeviceEntity : RealmObject {
    @PrimaryKey
    var uuid: String = ""
    var name: String = ""
    var description: String = ""
    var sensors: RealmList<SensorTypeEntity> = realmListOf()
    var taskTypes: RealmList<TaskTypeEntity> = realmListOf()
    var lastOnlineMillis: Long = 0L
}

class ParameterEntity : RealmObject {
    @PrimaryKey
    var uid: Long = 0L
    var name: String = ""
    var type: String = ""
    var unit: String = ""
    var description: String = ""
    var constraints: RealmList<KeyValueEntity> = realmListOf()
}

class ParameterSensorEntity : RealmObject {
    @PrimaryKey
    var uid: Long = 0L
    var name: String = ""
    var type: String = ""
    var unit: String = ""
    var description: String = ""
    var constraints: RealmList<KeyValueEntity> = realmListOf()
}

class ParameterTaskEntity : RealmObject {
    @PrimaryKey
    var uid: Long = 0L
    var name: String = ""
    var type: String = ""
    var unit: String = ""
    var description: String = ""
    var constraints: RealmList<KeyValueEntity> = realmListOf()
}

class SensorDataEntity : RealmObject {
    var deviceUuid: String = ""
    var sensorTypeUid: Long = 0L
    var value: String = ""
    var timestamp: Long = 0L
}

class SensorTypeEntity : RealmObject {
    @PrimaryKey
    var uid: Long = 0L
    var name: String = ""
    var description: String = ""
    var parameters: RealmList<ParameterSensorEntity> = realmListOf()
}

class TaskEntity : RealmObject {
    @PrimaryKey
    var uid: Long = 0L
    var deviceUuid: String = ""
    var taskTypeUid: Long = 0L
    var parametersValues: RealmList<KeyValueEntity> = realmListOf()
    var stateInfos: RealmList<TaskStateInfoEntity> = realmListOf()
}

class TaskStateInfoEntity : RealmObject {
    @PrimaryKey
    var id: String = ""
    var deviceUuid: String = ""
    var taskTypeUid: Long = 0L
    var taskUid: Long = 0L
    var dateTime: Long = 0L
    var state: String = ""
    var progress: Double = 0.0
    var errorMessage: String = ""
}

class TaskTypeEntity : RealmObject {
    @PrimaryKey
    var uid: Long = 0L
    var name: String = ""
    var description: String = ""
    var parameters: RealmList<ParameterTaskEntity> = realmListOf()
    var realTime: Boolean = false
}

class KeyValueEntity : RealmObject {
    var key: String = ""
    var value: String = ""
}

open class SensorDataRealm : RealmObject {
    // TODO remove, in this schema all tables are independent so far
    @PrimaryKey
    var id: String = UUID.randomUUID().toString() // TODO check RealmStorageType.UUID

    @Index
    @Required
    var deviceUuid: String = ""

    @Index
    @Required
    var sensorTypeUid: Long = 0

    var value: String = ""

    var timestampMillis: Long = 0L
}
