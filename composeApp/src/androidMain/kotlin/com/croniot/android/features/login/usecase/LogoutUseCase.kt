package com.croniot.android.features.login.usecase

import com.croniot.android.core.data.source.local.DataStoreController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LogoutUseCase {
    operator fun invoke() {
        CoroutineScope(Dispatchers.IO).launch {
            DataStoreController.clearAllCacheExceptDeviceUuid()
        }
    }
}
