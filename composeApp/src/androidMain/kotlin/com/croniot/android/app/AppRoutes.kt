package com.croniot.android.app

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable data object Test : AppRoute

    @Serializable data object Splash : AppRoute

    @Serializable data object Login : AppRoute

    @Serializable data object Devices : AppRoute

    @Serializable data object CreateAccount : AppRoute

    @Serializable data object Configuration : AppRoute

    @Serializable data class Device(val deviceUuid: String) : AppRoute

    @Serializable data class CreateTask(val deviceUuid: String, val taskUid: Long) : AppRoute
}
