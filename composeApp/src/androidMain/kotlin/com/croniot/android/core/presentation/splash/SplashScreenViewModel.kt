package com.croniot.android.core.presentation.splash

import Outcome
import androidx.lifecycle.ViewModel
import com.croniot.android.app.AppError
import com.croniot.client.domain.models.auth.AuthTokens
import com.croniot.client.domain.models.toUserMessage
import com.croniot.client.domain.repositories.AppSessionRepository
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.domain.repositories.SessionRepository
import com.croniot.client.domain.usecases.LogoutUseCase
import com.croniot.client.domain.usecases.StartDeviceListenersUseCase
import com.croniot.client.presentation.viewmodel.launchInVmScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class SplashScreenViewModel(
    private val localDataRepository: LocalDataRepository,
    private val sessionRepository: SessionRepository,
    private val logOutUseCase: LogoutUseCase,
    private val startDeviceListenersUseCase: StartDeviceListenersUseCase,
    private val appSessionRepository: AppSessionRepository,
) : ViewModel() {

    val effects: SharedFlow<SplashEffect>
        field = MutableSharedFlow(replay = 0, extraBufferCapacity = 1)

    fun initSplash() = launchInVmScope {
        val account = localDataRepository.getCurrentAccount()
        val tokens = sessionRepository.getTokens()

        if (account == null || tokens == null || !tokens.isAccessTokenValid()) {
            logOut()
            return@launchInVmScope
        }

        appSessionRepository.activateServerSession(account)
        val appError = when (val result = startDeviceListenersUseCase(account.devices)) {
            is Outcome.Err -> AppError(
                title = "Error de conexión",
                message = result.error.joinToString("\n") { it.toUserMessage() },
            )
            is Outcome.Ok -> null
        }
        navigateOnLogin(appError)
    }

    private fun AuthTokens.isAccessTokenValid(): Boolean {
        val nowSeconds = System.currentTimeMillis() / 1000L
        return expiresAtEpochSeconds > nowSeconds
    }

    private suspend fun navigateOnLogin(error: AppError?) {
        val selectedDeviceUuid = localDataRepository.getSelectedDevice()?.uuid

        val effect = if (selectedDeviceUuid != null) {
            SplashEffect.NavigateToDevice(selectedDeviceUuid, error)
        } else {
            SplashEffect.NavigateToDeviceList(error)
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
    data class NavigateToDeviceList(val error: AppError? = null) : SplashEffect
    data class NavigateToDevice(val deviceUuid: String, val error: AppError? = null) : SplashEffect
}
