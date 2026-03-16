package com.croniot.android.app

import androidx.lifecycle.ViewModel
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.presentation.viewmodel.launchInVmScope

class AppViewModel(
    private val localDataRepository: LocalDataRepository,
) : ViewModel() {

    init {
        launchInVmScope {
            localDataRepository.generateAndSaveDeviceUuidIfNotExists()
        }
    }

    fun onScreenChanged(route: String) = launchInVmScope {
        localDataRepository.saveCurrentScreen(route)
    }
}
