package com.croniot.android.core.presentation.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.croniot.android.app.AppError
import org.koin.androidx.compose.koinViewModel

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDeviceList: (error: AppError?) -> Unit,
    onNavigateToDevice: (deviceUuid: String, error: AppError?) -> Unit,
    splashScreenViewModel: SplashScreenViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) {
        splashScreenViewModel.initSplash()

        splashScreenViewModel.effects.collect { effect ->
            when (effect) {
                is SplashEffect.NavigateToLogin -> onNavigateToLogin()
                is SplashEffect.NavigateToDeviceList -> onNavigateToDeviceList(effect.error)
                is SplashEffect.NavigateToDevice -> onNavigateToDevice(effect.deviceUuid, effect.error)
            }
        }
    }
}
