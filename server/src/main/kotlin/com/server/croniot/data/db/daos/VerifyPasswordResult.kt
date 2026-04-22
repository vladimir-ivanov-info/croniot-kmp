package com.server.croniot.data.db.daos

sealed interface VerifyPasswordResult {
    data object UserNotFound : VerifyPasswordResult
    data object Invalid : VerifyPasswordResult
    data class Valid(val rehashed: Boolean) : VerifyPasswordResult
}