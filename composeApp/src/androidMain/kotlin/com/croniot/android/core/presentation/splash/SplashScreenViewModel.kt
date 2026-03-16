package com.croniot.android.core.presentation.splash

import Outcome
import androidx.lifecycle.ViewModel
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.domain.usecases.LogInUseCase
import com.croniot.client.domain.usecases.LogoutUseCase
import com.croniot.client.domain.usecases.StartDeviceListenersUseCase
import com.croniot.client.presentation.viewmodel.launchInVmScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class SplashScreenViewModel(
    private val localDataRepository: LocalDataRepository,
    private val logInUseCase: LogInUseCase,
    private val logOutUseCase: LogoutUseCase,
    private val startDeviceListenersUseCase: StartDeviceListenersUseCase,
) : ViewModel() {

    val effects: SharedFlow<SplashEffect>
        field = MutableSharedFlow(replay = 0, extraBufferCapacity = 1)

    fun initSplash() = launchInVmScope {
        val account = localDataRepository.getCurrentAccount()
        val password = localDataRepository.getCurrentPassword()

        if (account == null || password == null) {
            logOut()
            return@launchInVmScope
        }

        when (logInUseCase(email = account.email, password = password)) {
            is Outcome.Ok -> {
                navigateOnLogin()
                startDeviceListenersUseCase(account.devices)
            }
            else -> logOut()
        }
    }

    private suspend fun navigateOnLogin() {
        val selectedDeviceUuid = localDataRepository.getSelectedDevice()?.uuid

        val effect = if (selectedDeviceUuid != null) {
            SplashEffect.NavigateToDevice(selectedDeviceUuid)
        } else {
            SplashEffect.NavigateToDeviceList
        }

        effects.tryEmit(effect)
    }

    private suspend fun logOut() {
        logOutUseCase()
        effects.tryEmit(SplashEffect.NavigateToLogin)
    }
}

sealed interface SplashEffect {
    data object NavigateToLogin : SplashEffect
    data object NavigateToDeviceList : SplashEffect
    data class NavigateToDevice(val deviceUuid: String) : SplashEffect
}
