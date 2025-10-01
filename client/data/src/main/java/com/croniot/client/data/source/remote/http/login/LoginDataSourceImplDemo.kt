package com.croniot.client.data.source.remote.http.login

import croniot.messages.MessageLoginRequest
import croniot.models.LoginResultDto
import croniot.models.dto.AccountDto
import croniot.models.dto.DeviceDto
import croniot.models.dto.ParameterSensorDto
import croniot.models.dto.SensorTypeDto

class LoginDataSourceImplDemo(
    //private val api: LoginApi
) : LoginDataSource {

    override suspend fun login(request: MessageLoginRequest): Result<LoginResultDto> = runCatching {
        /*val resp = api.login(request)
        if (!resp.isSuccessful) {
            throw HttpException(resp) // o crea tu propio error
        }
        val dto = resp.body() ?: error("Empty body")
        dto //.toDomain() // mapea DTO → LoginResult
        */

        val loginResultDto = LoginResultDto(
            result = croniot.models.Result(true, "Success",),
            account = generateDemoAccountdto(),
            token = "token_demo"
        )

        return Result.success(loginResultDto)
    }


    private fun generateDemoAccountdto() : AccountDto {

        val accountDto = AccountDto(
            uuid = "account_demo",
            nickname = "Demo",
            email = "demo@email.com",
            devices = generateDemoDeviceDto()
        )

        return accountDto
    }

    private fun generateDemoDeviceDto() : List<DeviceDto> {

        val result = mutableListOf<DeviceDto>()

        val device1 = DeviceDto(
            uuid = "IoT Device 1",
            name = "House IoT device",
            description = "IoT device for controlling sensors and running tasks in my house.",
            sensorTypes = generateSensorTypeDtos(),
            taskTypes = emptyList()
        )

        result.add(device1)

        return result
    }

    private fun generateSensorTypeDtos() : List<SensorTypeDto>{

        val result : MutableList<SensorTypeDto> = mutableListOf()

        var parameters1 : MutableList<ParameterSensorDto> = mutableListOf()
        var parameterSensor1 = ParameterSensorDto(
            uid = 1,
            name =  "WiFi level",
            type = "integer",
            unit = "dBm",
            description = "WiFi level expressed in dBm",
            constraints = mapOf("minValue" to "-90", "maxValue" to "-30")
        )

        parameters1.add(parameterSensor1)

        val sensor1 = SensorTypeDto(
            uid = 1L,
            name = "WiFi level",
            description = "My IoT device's WiFi singnal level",
            parameters = parameters1
        )



        result.add(sensor1)

        return result
    }
}