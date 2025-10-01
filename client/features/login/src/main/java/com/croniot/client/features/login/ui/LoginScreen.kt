package com.croniot.client.features.login.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.croniot.client.core.Global
import com.croniot.client.presentation.constants.UiConstants
import com.croniot.client.features.login.R
import com.croniot.client.presentation.components.EmailTextField
import com.croniot.client.presentation.components.PasswordTextField
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreenRoot(
    onNavigate: (route: String) -> Unit,
    loginViewModel: LoginViewModel = koinViewModel(),
){
    val state = loginViewModel.state.collectAsStateWithLifecycle()
    val effects = loginViewModel.effects

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        effects.collectLatest { effect ->
            when (effect) {
                is LoginEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = effect.content,
                        withDismissAction = true
                    )
                }
                LoginEffect.NavigateHome -> onNavigate(UiConstants.ROUTE_DEVICES)
                LoginEffect.NavigateToRegisterAccount -> onNavigate(UiConstants.ROUTE_CREATE_ACCOUNT)
                LoginEffect.NavigateToConfiguration -> onNavigate(UiConstants.ROUTE_CONFIGURATION)
            }
        }
    }

    LoginScreen(
        state = state,
        onAction =  loginViewModel::onAction,
        serverMode = "local", //TODO,
        snackbarHostState = snackbarHostState
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    state: State<LoginState>,
    onAction: (LoginIntent) -> Unit,
    serverMode: String,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(text = Global.appName,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        },
        content = { innerPadding ->
            LoginScreenBody(
                state = state,
                innerPadding = innerPadding,
                serverMode = serverMode,
                onAction = onAction
            )
        },
    )
}

@Composable
fun LoginScreenBody(
    state: State<LoginState>,
    innerPadding: PaddingValues,
    serverMode: String,
    onAction: (LoginIntent) -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                    )
                ),
                shape = MaterialTheme.shapes.medium
            )
            ,
    ) {
        LoginContent(
            state = state,
            serverMode = serverMode,
            modifier = Modifier
                .align(Alignment.TopCenter)
            ,
            onAction = onAction
        )
    }
}

@Composable
fun LoginContent(
    state: State<LoginState>,
    serverMode: String,
    modifier: Modifier,
    onAction: (LoginIntent) -> Unit,
) {
    Column(
        modifier = modifier
            .padding(16.dp)
        ,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        SlowHeroSlogan()

        // Logo
        Box(
            modifier = Modifier
                .size(200.dp)
                .clickable {
                    onAction(LoginIntent.GoToConfigurationScreen)
                },
            contentAlignment = Alignment.Center,
        ) {
            val logoResourceId = if (serverMode == "local") {
                R.drawable.logo_cockroach_test_mode
            } else {
                R.drawable.logo_cockroach
            }

            Image(
                painter = painterResource(id = logoResourceId),
                contentDescription = null,
                modifier = Modifier.clip(CircleShape),
                contentScale = ContentScale.Fit,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Login",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .align(Alignment.Start),
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.size(4.dp))

        EmailTextField(
            value = state.value.email,
            enabled = !state.value.isLoading,
            onValueChange = {
                onAction(LoginIntent.EmailChanged(it))
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordTextField(
            value = state.value.password,
            enabled = !state.value.isLoading,
            onValueChange = {
                onAction(LoginIntent.PasswordChanged(it))
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        LoginButton(
            state = state,
            onAction = onAction
        )

        /*Spacer(modifier = Modifier.height(16.dp))

        LoginAsGuestButton(
            state = state,
            onClick = {
                onAction(LoginIntent.LoginAsGuest)
            }
        )*/

        Spacer(modifier = Modifier.height(16.dp))

        RegisterButton(
            state = state,
            onAction = onAction
        )
    }
}

@Composable
fun LoginButton(
    state: State<LoginState>,
    onAction: (LoginIntent) -> Unit
) {
    Button(
        onClick = {
            onAction(LoginIntent.Login)
        },
        modifier = Modifier
            .fillMaxWidth()
    ) {
        if (state.value.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp),   // tamaño reducido para que encaje bien en el botón
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary // color visible sobre el fondo
            )
        } else {
            Text(text = "Log in")
        }
    }
}

/*@Composable
fun LoginAsGuestButton(
    state: State<LoginState>,
    onClick: () -> Unit
) {
    Button(
        onClick = { onClick() },
        modifier = Modifier
            .fillMaxWidth(),

        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,  // color de fondo
            contentColor = MaterialTheme.colorScheme.surfaceBright,          // color del texto e iconos
            //disabledContainerColor = Color.Gray, // color de fondo si está deshabilitado
            //disabledContentColor = Color.LightGray // color de texto si está deshabilitado
        )
    ) {
        if (state.value.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp),   // tamaño reducido para que encaje bien en el botón
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary // color visible sobre el fondo
            )
        } else {
            Text(text = "Log in as guest")
        }
    }
}*/


@Composable
fun RegisterButton(
    state: State<LoginState>,
    onAction: (LoginIntent) -> Unit,
) {
    Button(
        enabled = !state.value.isLoading,
        onClick = {
            onAction(LoginIntent.GoToCreateAccountScreen)
        },
        modifier = Modifier
            .testTag(UiConstants.SCREEN_LOGIN_BUTTON_REGISTER_TAG)
        ,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.secondary,
        )
    ) {
        Text(
            text = UiConstants.SCREEN_LOGIN_BUTTON_CREATE_ACCOUNT_TEXT
        )
    }
}


@Composable
fun SlowHeroSlogan() {
    val alpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val offsetY = remember { androidx.compose.animation.core.Animatable(16f) }

    LaunchedEffect(Unit) {
        delay(800) // pausa para crear expectativa, pero el Text ya ocupa sitio
        // animaciones en paralelo, lentas y suaves
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 3000, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 3000, easing = LinearOutSlowInEasing)
            )
        }
    }

    Text(
        text = "The future of IoT. In your hands.",
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Light,
            letterSpacing = 0.8.sp,
            lineHeight = 28.sp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 24.dp)
            .graphicsLayer { translationY = offsetY.value } // ocupa sitio desde el inicio
            .alpha(alpha.value)                              // se hace visible lentamente
    )
}