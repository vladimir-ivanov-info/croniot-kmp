package com.croniot.android.features.login.domain.repository

import croniot.messages.MessageLoginRequest
import croniot.models.LoginResult

interface LoginRepository {
    suspend fun login(request: MessageLoginRequest): LoginResult
    suspend fun logout()

    fun getDeviceUuid(): String?
    fun getDeviceToken(): String?
    fun getDeviceProperties():  Map<String, String>
}
