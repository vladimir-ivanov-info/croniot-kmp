package com.croniot.client.features.login.presentation

import com.croniot.client.domain.models.toUserMessage
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.croniot.client.core.config.AppConfig
import com.croniot.client.features.login.R
import com.croniot.client.presentation.components.AppBackground
import com.croniot.client.presentation.components.EmailTextField
import com.croniot.client.presentation.components.PasswordTextField
import com.croniot.client.presentation.constants.UiConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onNavigateToDeviceList: () -> Unit,
    onNavigateToRegisterAccount: () -> Unit,
    onNavigateToConfiguration: () -> Unit,
    onNavigateToBleDiscovery: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val effects = viewModel.effects

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        effects.collect { effect ->
            when (effect) {
                is LoginEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = effect.content,
                        withDismissAction = true,
                    )
                }
                LoginEffect.NavigateHome -> onNavigateToDeviceList()
                LoginEffect.NavigateToRegisterAccount -> onNavigateToRegisterAccount()
                LoginEffect.NavigateToConfiguration -> onNavigateToConfiguration()
                LoginEffect.NavigateToBleDiscovery -> onNavigateToBleDiscovery()
                is LoginEffect.ConnectionErrors -> {
                    val message = effect.errors.joinToString("\n") { it.toUserMessage() }
                    snackbarHostState.showSnackbar(
                        message = message,
                        withDismissAction = true,
                    )
                }
            }
        }
    }

    LoginScreenBody(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun LoginScreenBody(
    state: State<LoginState>,
    onAction: (LoginIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            content = { innerPadding ->
                LoginScreenContent(
                    state = state,
                    innerPadding = innerPadding,
                    onAction = onAction,
                )
            },
        )
    }
}

@Composable
fun LoginScreenContent(
    state: State<LoginState>,
    innerPadding: PaddingValues,
    onAction: (LoginIntent) -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .imePadding(),
    ) {
        LoginContent(
            state = state,
            modifier = Modifier
                .fillMaxSize(),
            onAction = onAction,
        )
    }
}

@Composable
fun LoginContent(
    state: State<LoginState>,
    modifier: Modifier,
    onAction: (LoginIntent) -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SlowHeroSlogan()

        // Logo
        Box(
            modifier = Modifier
                .size(200.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    onAction(LoginIntent.GoToConfigurationScreen)
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_croniot_1),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .padding(24.dp),
                contentScale = ContentScale.Fit,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = AppConfig.appName.lowercase(),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Light,
                letterSpacing = 6.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        EmailTextField(
            value = state.value.email,
            enabled = !state.value.isLoading,
            onValueChange = {
                onAction(LoginIntent.EmailChanged(it))
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordTextField(
            value = state.value.password,
            enabled = !state.value.isLoading,
            onValueChange = {
                onAction(LoginIntent.PasswordChanged(it))
            },
        )

        Spacer(modifier = Modifier.height(24.dp))

        LoginButton(
            state = state,
            onAction = onAction,
        )

        Spacer(modifier = Modifier.height(16.dp))

        RegisterButton(
            state = state,
            onAction = onAction,
        )

        Spacer(modifier = Modifier.height(24.dp))

        BleDiscoveryEntryPoint(
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
fun BleDiscoveryEntryPoint(
    state: State<LoginState>,
    onAction: (LoginIntent) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "─── o ───",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            enabled = !state.value.isLoading,
            onClick = { onAction(LoginIntent.GoToBleDiscovery) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Conectar a un dispositivo cercano →",
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun LoginButton(
    state: State<LoginState>,
    onAction: (LoginIntent) -> Unit,
) {
    Button(
        enabled = !state.value.isLoading,
        onClick = {
            onAction(LoginIntent.Login)
        },
        modifier = Modifier
            .testTag("login_screen_login_button")
            .fillMaxWidth(),
    ) {
        if (state.value.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(text = "Log in")
        }
    }
}

@Composable
fun RegisterButton(
    state: State<LoginState>,
    onAction: (LoginIntent) -> Unit,
) {
    TextButton(
        enabled = !state.value.isLoading,
        onClick = {
            onAction(LoginIntent.GoToCreateAccountScreen)
        },
        modifier = Modifier
            .testTag(UiConstants.SCREEN_LOGIN_BUTTON_REGISTER_TAG),
    ) {
        Text(
            text = stringResource(R.string.create_account),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)

        )
    }
}

@Composable
fun SlowHeroSlogan() {
    val alpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val offsetY = remember { androidx.compose.animation.core.Animatable(16f) }

    LaunchedEffect(Unit) {
        delay(800)
        // animaciones en paralelo, lentas y suaves
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 3000, easing = LinearOutSlowInEasing),
            )
        }
        launch {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 3000, easing = LinearOutSlowInEasing),
            )
        }
    }

    Text(
        text = stringResource(R.string.login_slogan),
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Light,
            letterSpacing = 0.8.sp,
            lineHeight = 28.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 24.dp)
            .graphicsLayer { translationY = offsetY.value }
            .alpha(alpha.value),
    )
}
