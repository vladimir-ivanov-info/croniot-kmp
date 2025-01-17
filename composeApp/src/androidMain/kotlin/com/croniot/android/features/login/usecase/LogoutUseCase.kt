package com.croniot.android.features.login.usecase

import com.croniot.android.core.data.source.local.SharedPreferences

class LogoutUseCase {
    operator fun invoke() {
        SharedPreferences.clearCache()
    }
}