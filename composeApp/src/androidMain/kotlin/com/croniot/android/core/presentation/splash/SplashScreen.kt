package com.croniot.android.core.presentation.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import org.koin.androidx.compose.koinViewModel

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDeviceList: () -> Unit,
    onNavigateToDevice: (deviceUuid: String) -> Unit,
    splashScreenViewModel: SplashScreenViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) {
        splashScreenViewModel.initSplash()

        splashScreenViewModel.effects.collect { effect ->
            when (effect) {
                is SplashEffect.NavigateToLogin -> onNavigateToLogin()
                is SplashEffect.NavigateToDeviceList -> onNavigateToDeviceList()
                is SplashEffect.NavigateToDevice -> onNavigateToDevice(effect.deviceUuid)
            }
        }
    }
}
