package com.croniot.client.data.source.remote.http.login

import com.croniot.client.data.strategy.DataSourceStrategy
import com.croniot.client.data.strategy.DataSourceStrategyBus
import croniot.messages.MessageLoginRequest
import croniot.models.LoginResultDto

class StrategyLoginDataSource(
    private val realLoginDataSource: LoginDataSource,
    private val demoLoginDataSource: LoginDataSource,
    private val bus: DataSourceStrategyBus,
) : LoginDataSource {

    override suspend fun login(request: MessageLoginRequest): Result<LoginResultDto> {
        val active = if (bus.current.value == DataSourceStrategy.DEMO) demoLoginDataSource else realLoginDataSource
        return active.login(request)
    }
}
