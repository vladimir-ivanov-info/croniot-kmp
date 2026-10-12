package com.server.croniot.application

import com.server.croniot.controllers.AccountController
import com.server.croniot.controllers.AdminDirectoryController
import com.server.croniot.controllers.AdminLogController
import com.server.croniot.controllers.AdminLogStreamController
import com.server.croniot.controllers.DeviceController
import com.server.croniot.controllers.DeviceLogConfigController
import com.server.croniot.controllers.DeviceLogController
import com.server.croniot.controllers.FeatureFlagController
import com.server.croniot.controllers.LoginController
import com.server.croniot.controllers.SensorTypeController
import com.server.croniot.controllers.TaskController
import com.server.croniot.controllers.TaskTypeController
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.http.SensorsDataController
import com.server.croniot.services.DeviceLogService
import com.server.croniot.services.DeviceStatusService
import com.server.croniot.services.SensorBatchService
import dagger.Component
import org.jooq.DSLContext
import javax.inject.Singleton
import javax.sql.DataSource

@Singleton
@Component(
    modules = [
        AppModule::class,
        // Add other modules as needed
    ],
)
interface AppComponent {
    fun loginController(): LoginController
    fun accountController(): AccountController
    fun deviceController(): DeviceController
    fun taskController(): TaskController
    fun sensorTypeController(): SensorTypeController
    fun taskTypeController(): TaskTypeController
    fun sensorDataController(): SensorsDataController
    fun jwtConfig(): JwtConfig
    fun applicationScope(): ApplicationScope

    fun deviceRepository(): DeviceRepository
    fun featureFlagController(): FeatureFlagController
    fun dataSource(): DataSource
    fun dslContext(): DSLContext

    fun deviceLogConfigController(): DeviceLogConfigController
    fun deviceLogController(): DeviceLogController
    fun adminLogStreamController(): AdminLogStreamController
    fun adminLogController(): AdminLogController
    fun adminDirectoryController(): AdminDirectoryController
    fun deviceLogService(): DeviceLogService
    fun sensorBatchService(): SensorBatchService
    fun deviceStatusService(): DeviceStatusService
}
