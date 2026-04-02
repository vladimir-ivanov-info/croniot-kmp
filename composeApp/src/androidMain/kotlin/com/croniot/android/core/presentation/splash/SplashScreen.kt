package com.croniot.android.core.presentation.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.croniot.android.R
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_croniot_2),
            contentDescription = null,
            modifier = Modifier.width(160.dp),
            contentScale = ContentScale.Fit,
        )
    }
}
