package com.croniot.client.data.source.remote.http

import croniot.models.Result

interface NetworkUtil {
    suspend fun post(endPoint: String, postData: String): Result
}
