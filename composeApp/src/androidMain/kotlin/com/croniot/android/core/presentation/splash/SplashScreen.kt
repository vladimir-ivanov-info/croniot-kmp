package com.croniot.android.core.presentation.splash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.croniot.client.presentation.constants.UiConstants
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplashScreen(
    navController: NavController,
    splashScreenViewModel: SplashScreenViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) {
        splashScreenViewModel.initSplash()
    }

    LaunchedEffect(Unit) {
        splashScreenViewModel.uiEvents.collect { event ->
            when (event) {
                is SplashScreenUiEvent.NavigateToDeviceList -> {
                    navController.navigate(UiConstants.ROUTE_DEVICES) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                is SplashScreenUiEvent.NavigateToDevice -> {
                    navController.navigate("${UiConstants.ROUTE_DEVICE}/${event.deviceUuid}") {
                        popUpTo(0) { inclusive = true }
                    }
                }
                is SplashScreenUiEvent.NavigateToLogin -> {
                    navController.navigate(UiConstants.ROUTE_LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    BackHandler {
        if (!navController.popBackStack()) {
            // viewModel.resetCurrentScreen() //TODO
            navController.navigate(UiConstants.ROUTE_DEVICES)
        }
    }

    SideEffect {
        // viewModel.saveCurrentScreen() //TODO
        // viewModelDeviceScreen.saveCurrentScreen()
    }

    Scaffold(
        topBar = {
            TopAppBar( // This material API is experimental and is likely to change or to be removed in the future.
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(contentAlignment = Alignment.CenterStart) {
                            Text(text = "Splash")
                        }
                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        },
        content = { innerPadding ->
            SplashScreenContent(
                navController = navController,
                innerPadding = innerPadding,
            )
        },
    )
}

@Composable
fun SplashScreenContent(
    navController: NavController,
    innerPadding: PaddingValues,
) {
    // Nothing
}
