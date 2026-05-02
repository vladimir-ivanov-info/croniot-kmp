package com.croniot.client.data.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.croniot.client.data.repositories.AccountRepositoryImpl
import com.croniot.client.data.repositories.AppSessionRepositoryImpl
import com.croniot.client.data.repositories.AuthRepositoryImpl
import com.croniot.client.data.repositories.BleDevicesRepositoryImpl
import com.croniot.client.data.repositories.SensorDataRepositoryImpl
import com.croniot.client.data.repositories.SessionRepositoryImpl
import com.croniot.client.data.repositories.TaskTypesRepositoryImpl
import com.croniot.client.data.source.local.AppPreferencesLocalDatasource
import com.croniot.client.data.source.local.AuthLocalDatasource
import com.croniot.client.data.source.local.DataStoreController
import com.croniot.client.data.source.local.DeviceLocalDatasource
import com.croniot.client.data.source.local.EncryptedTokenStore
import com.croniot.client.data.source.local.NavigationLocalDatasource
import com.croniot.client.data.source.local.ServerConfigLocalDatasource
import com.croniot.client.data.source.local.TokenStore
import com.croniot.client.data.source.local.ble.BleCredentialStore
import com.croniot.client.data.source.local.ble.EncryptedBleCredentialStore
import com.croniot.client.data.source.local.database.AppDatabase
import com.croniot.client.data.source.remote.ble.BleConnectionPool
import com.croniot.client.data.source.remote.ble.BleConnectionPoolImpl
import com.croniot.client.data.source.remote.ble.BlePermissionsHelper
import com.croniot.client.data.source.remote.ble.BlePermissionsHelperImpl
import com.croniot.client.data.source.remote.ble.BleScanner
import com.croniot.client.data.source.remote.ble.BleScannerImpl
import com.croniot.client.data.source.remote.ble.BleTasksDataSourceImpl
import com.croniot.client.data.source.remote.http.login.LoginDataSource
import com.croniot.client.data.source.remote.http.login.LoginDataSourceImpl
import com.croniot.client.data.source.remote.mqtt.TasksDataSource
import com.croniot.client.data.source.remote.mqtt.TasksDataSourceImpl
import com.croniot.client.data.source.sensors.BleSensorDataSourceImpl
import com.croniot.client.data.source.sensors.LocalSensorDataSource
import com.croniot.client.data.source.sensors.LocalSensorDataSourceRoomImpl
import com.croniot.client.data.source.sensors.RemoteSensorDataSource
import com.croniot.client.data.source.sensors.RemoteSensorDataSourceImpl
import com.croniot.client.data.source.taskhistory.LocalTaskHistoryDataSource
import com.croniot.client.data.source.taskhistory.LocalTaskHistoryDataSourceRoomImpl
import com.croniot.client.data.source.transport.TransportRouter
import com.croniot.client.data.source.transport.TransportRouterImpl
import com.croniot.client.domain.repositories.AccountRepository
import com.croniot.client.domain.repositories.AppSessionRepository
import com.croniot.client.domain.repositories.AuthRepository
import com.croniot.client.domain.repositories.BleDevicesRepository
import com.croniot.client.domain.repositories.SensorDataRepository
import com.croniot.client.domain.repositories.SessionRepository
import com.croniot.client.domain.repositories.TaskTypesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_task_history_cache_deviceUuid_taskTypeUid_timeStampMillis` " +
                "ON `task_history_cache` (`deviceUuid`, `taskTypeUid`, `timeStampMillis`)"
        )
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `ble_known_devices` ADD COLUMN `schemaVersion` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `ble_known_devices` ADD CO      LUMN `schemaJson` TEXT DEFAULT NULL")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ble_known_devices` (
                `uuid` TEXT NOT NULL,
                `displayName` TEXT NOT NULL,
                `macAddress` TEXT NOT NULL,
                `lastSeenAtMillis` INTEGER NOT NULL,
                `addedAtMillis` INTEGER NOT NULL,
                PRIMARY KEY(`uuid`)
            )
            """.trimIndent()
        )
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `task_history_cache` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `deviceUuid` TEXT NOT NULL,
                `stateInfoId` INTEGER,
                `taskUid` INTEGER NOT NULL,
                `taskTypeUid` INTEGER NOT NULL,
                `timeStampMillis` INTEGER NOT NULL,
                `state` TEXT NOT NULL,
                `progress` REAL NOT NULL,
                `errorMessage` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_history_cache_deviceUuid` ON `task_history_cache` (`deviceUuid`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_history_cache_deviceUuid_timeStampMillis` ON `task_history_cache` (`deviceUuid`, `timeStampMillis`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_task_history_cache_deviceUuid_stateInfoId` ON `task_history_cache` (`deviceUuid`, `stateInfoId`)")
    }
}

val dataModule = module {

    single(named("appScope")) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    single<AppDatabase> {
        Room.databaseBuilder(
            context = androidContext(),
            klass = AppDatabase::class.java,
            name = "croniot.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
    }

    single { get<AppDatabase>().sensorDataDao() }
    single { get<AppDatabase>().taskHistoryCacheDao() }
    single { get<AppDatabase>().bleKnownDeviceDao() }

    single<BleCredentialStore> { EncryptedBleCredentialStore(context = androidContext()) }

    single<TransportRouter> {
        TransportRouterImpl(bleKnownDeviceDao = get()).also { router ->
            get<CoroutineScope>(named("appScope")).launch { router.loadInitial() }
        }
    }

    single<AppSessionRepository> {
        AppSessionRepositoryImpl(
            localDataRepository = get(),
            appScope = get(named("appScope")),
        )
    }

    single { DataStoreController(context = get()) }
    single<ServerConfigLocalDatasource> { get<DataStoreController>() }
    single<AuthLocalDatasource> { get<DataStoreController>() }
    single<DeviceLocalDatasource> { get<DataStoreController>() }
    single<NavigationLocalDatasource> { get<DataStoreController>() }
    single<AppPreferencesLocalDatasource> { get<DataStoreController>() }

    single<TokenStore> { EncryptedTokenStore(context = androidContext()) }

    single<AuthRepository> { AuthRepositoryImpl(loginDataSource = get()) }

    single<AccountRepository> { AccountRepositoryImpl(localDataSource = get()) }

    single<SessionRepository> {
        SessionRepositoryImpl(
            authLocalDatasource = get(),
            appPreferencesLocalDatasource = get(),
            tokenStore = get(),
        )
    }

    single<TasksDataSource> {
        TasksDataSourceImpl(
            taskApi = get(),
            localDatasource = get(),
            appScope = get(named("appScope")),
        )
    }

    single<LoginDataSource> {
        LoginDataSourceImpl(api = get())
    }

    single<TaskTypesRepository> { TaskTypesRepositoryImpl() }

    single<LocalSensorDataSource> {
        LocalSensorDataSourceRoomImpl(sensorDataDao = get())
    }

    single<RemoteSensorDataSource> {
        RemoteSensorDataSourceImpl(
            appScope = get(named("appScope")),
            localDatasource = get(),
        )
    }

    single<BleConnectionPool> { BleConnectionPoolImpl(context = androidContext()) }

    single<BlePermissionsHelper> { BlePermissionsHelperImpl(context = androidContext()) }

    single<BleScanner> {
        BleScannerImpl(
            context = androidContext(),
            permissionsHelper = get(),
        )
    }

    single<BleDevicesRepository> {
        BleDevicesRepositoryImpl(
            context = androidContext(),
            scanner = get(),
            connectionPool = get(),
            credentialStore = get(),
            bleKnownDeviceDao = get(),
            transportRouter = get(),
            appScope = get(named("appScope")),
        )
    }

    single<RemoteSensorDataSource>(named("ble")) {
        BleSensorDataSourceImpl(
            appScope = get(named("appScope")),
            connectionPool = get(),
        )
    }

    single<TasksDataSource>(named("ble")) {
        BleTasksDataSourceImpl(
            appScope = get(named("appScope")),
            connectionPool = get(),
        )
    }

    single<LocalTaskHistoryDataSource> {
        LocalTaskHistoryDataSourceRoomImpl(
            taskHistoryCacheDao = get(),
        )
    }

    single<SensorDataRepository> {
        SensorDataRepositoryImpl(
            cloudSensorDataSource = get(),
            bleSensorDataSource = get(named("ble")),
            transportRouter = get(),
            localSensorDataSource = get(),
            scope = get(named("appScope")),
        )
    }
}
