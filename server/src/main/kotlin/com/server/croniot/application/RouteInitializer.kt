package com.server.croniot.application

import com.server.croniot.controllers.*
import croniot.messages.MessageFactory
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sse.sse
import java.time.LocalDateTime
import javax.inject.Inject

class RouteInitializer @Inject constructor(
    private val loginController: LoginController,
    private val accountController: AccountController,
    private val deviceController: DeviceController,
    private val taskController: TaskController,
    private val sensorTypeController: SensorTypeController,
    private val taskTypeController: TaskTypeController,
    private val featureFlagController: FeatureFlagController,
    private val deviceLogConfigController: DeviceLogConfigController,
    private val deviceLogController: DeviceLogController,
    private val adminLogStreamController: AdminLogStreamController,
    private val adminLogController: AdminLogController,
    private val adminDirectoryController: AdminDirectoryController,
    private val adminServerLogLevelController: AdminServerLogLevelController,
    private val otaController: OtaController,
) {
    fun setupRoutes(application: Application) {
        val prometheusRegistry = application.attributes.getOrNull(PROMETHEUS_REGISTRY_KEY)
        application.routing {
            if (prometheusRegistry != null) {
                get("/metrics") {
                    call.respondText(prometheusRegistry.scrape(), ContentType.parse("text/plain; version=0.0.4"))
                }
            }

            post("/dateTime") {
                val currentDateTime = LocalDateTime.now()
                val hour = currentDateTime.hour
                val minute = currentDateTime.minute
                val response = "$hour:$minute"

                val result = croniot.models.Result(true, response)
                val responseJson = MessageFactory.toJson(result)
                call.respondText(responseJson, ContentType.Application.Json)
            }

            post("/hour") {
                val currentDateTime = LocalDateTime.now()
                val hour = currentDateTime.hour
                val response = "$hour"
                val result = croniot.models.Result(true, response)
                val responseJson = MessageFactory.toJson(result)
                call.respondText(responseJson, ContentType.Application.Json)
            }

            post("/minute") {
                val currentDateTime = LocalDateTime.now()
                val minute = currentDateTime.minute
                val response = "$minute"
                val result = croniot.models.Result(true, response)
                val responseJson = MessageFactory.toJson(result)
                call.respondText(responseJson, ContentType.Application.Json)
            }

            post("/second") {
                val currentDateTime = LocalDateTime.now()
                val second = currentDateTime.second
                val response = "$second"
                val result = croniot.models.Result(true, response)
                val responseJson = MessageFactory.toJson(result)
                call.respondText(responseJson, ContentType.Application.Json)
            }

            rateLimit(RATE_LIMIT_AUTH) {
                post("/api/login") {
                    loginController.login(call)
                }

                post("/api/iot/login") {
                    loginController.loginIot(call)
                }

                post("/api/token/refresh") {
                    loginController.refreshToken(call)
                }

                post("/api/register_account") {
                    accountController.registerAccount(call)
                }
            }

            post("/api/logout") {
                loginController.logout(call)
            }

            get("/taskConfiguration/{deviceUuid}") {
                taskController.getTaskConfigurations(call)
            }

            get("/taskStateInfoHistory/{deviceUuid}") {
                taskController.getTaskStateInfoHistory(call)
            }

            get("/taskStateInfoHistoryCount/{deviceUuid}") {
                taskController.getTaskStateInfoHistoryCount(call)
            }

            post("/api/register_client") {
                deviceController.registerDevice(call)
            }

            post("/api/register_sensor_type") {
                sensorTypeController.registerSensorType(call)
            }

            post("/api/register_task_type") {
                taskTypeController.registerTaskType(call)
            }

            post("/api/add_task") {
                taskController.addTask(call)
            }

            post("/api/request_task_state_info_sync") {
                taskController.requestTaskStateInfoSync(call)
            }

            get("/api/feature_flags") {
                featureFlagController.getAllFlags(call)
            }

            get("/api/devices/{uuid}/logs") {
                deviceLogController.getLogs(call)
            }

            get("/api/devices/{uuid}/events") {
                deviceLogController.getEvents(call)
            }

            // Authenticated by the device's own credentials (headers), not a JWT.
            get("/api/iot/firmware/{id}") {
                otaController.downloadFirmware(call)
            }

            authenticate(AUTH_JWT_REALM) {
                put("/api/admin/feature_flags/{name}") {
                    featureFlagController.setFlag(call)
                }

                put("/api/devices/{uuid}/log_config") {
                    deviceLogConfigController.putConfig(call)
                }

                get("/api/devices/{uuid}/log_config") {
                    deviceLogConfigController.getConfig(call)
                }

                route("/api/admin/logs/stream") {
                    install(AdminOnly)
                    sse { adminLogStreamController.stream(this) }
                }

                get("/api/admin/logs") {
                    call.requireAdmin()
                    adminLogController.getLogs(call)
                }

                get("/api/admin/devices") {
                    call.requireAdmin()
                    adminDirectoryController.listDevices(call)
                }

                get("/api/admin/devices/{uuid}") {
                    call.requireAdmin()
                    adminDirectoryController.getDevice(call)
                }

                get("/api/admin/accounts/{uuid}") {
                    call.requireAdmin()
                    adminDirectoryController.getAccount(call)
                }

                route("/api/admin/firmware") {
                    install(AdminOnly)
                    post { otaController.uploadFirmware(call) }
                    get { otaController.listFirmware(call) }
                }

                route("/api/admin/devices/{uuid}/ota") {
                    install(AdminOnly)
                    put { otaController.requestUpdate(call) }
                    get { otaController.getUpdate(call) }
                    delete { otaController.cancelUpdate(call) }
                }

                route("/api/admin/server/log_level") {
                    install(AdminOnly)
                    get { adminServerLogLevelController.get(call) }
                    put { adminServerLogLevelController.set(call) }
                    delete { adminServerLogLevelController.revert(call) }
                }
            }
        }
    }
}
