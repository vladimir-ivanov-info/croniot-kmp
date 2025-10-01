package com.croniot.client.data.strategy

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class DataSourceStrategy { REAL, DEMO }

interface DataSourceStrategyBus {
    val current: StateFlow<DataSourceStrategy>
    fun setDataSourceStrategy(strategy: DataSourceStrategy)
}

class MutableDataSourceStrategyBus(initial: DataSourceStrategy): DataSourceStrategyBus {
    private val _state = MutableStateFlow(initial)
    override val current: StateFlow<DataSourceStrategy> = _state
    override fun setDataSourceStrategy(strategy: DataSourceStrategy) { _state.value = strategy }
}

fun DataSourceStrategyBus.setDemo(enabled: Boolean) {
    val newStrategy = if (enabled) DataSourceStrategy.DEMO else DataSourceStrategy.REAL

    //TODO oldStrategy.disconnectAndClearAllCachesIfNeeded()

    setDataSourceStrategy(newStrategy)
}
