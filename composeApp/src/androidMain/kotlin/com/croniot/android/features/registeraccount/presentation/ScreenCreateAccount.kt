package com.croniot.android.features.registeraccount.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.croniot.android.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.croniot.client.presentation.components.EmailTextField
import com.croniot.client.presentation.components.PasswordTextField
import com.croniot.client.presentation.components.NicknameTextField
import com.croniot.client.presentation.constants.UiConstants
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScreenRegisterAccount(
    onNavigateBack: () -> Unit,
    viewModelRegisterAccount: ViewModelRegisterAccount = koinViewModel(),
) {
    val state = viewModelRegisterAccount.state.collectAsStateWithLifecycle()
    val effects = viewModelRegisterAccount.effects

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        effects.collectLatest { effect ->
            when (effect) {
                is CreateAccountEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = effect.content,
                        withDismissAction = true,
                    )
                }
                CreateAccountEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    BackHandler {
        onNavigateBack()
    }

    ScreenRegisterAccountBody(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = { action ->
            when (action) {
                is RegisterAccountIntent.NavigateBack -> {
                    onNavigateBack()
                }
                else -> viewModelRegisterAccount.onAction(action)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenRegisterAccountBody(
    state: State<CreateAccountState>,
    snackbarHostState: SnackbarHostState,
    onAction: (RegisterAccountIntent) -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(UiConstants.SCREEN_LOGIN_BUTTON_CREATE_ACCOUNT_TEXT) },
                navigationIcon = {
                    IconButton(onClick = { onAction(RegisterAccountIntent.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        content = { innerPadding ->
            ScreenRegisterAccountBody(
                state = state,
                innerPadding = innerPadding,
                onAction = onAction,
            )
        },
    )
}

@Composable
fun ScreenRegisterAccountBody(
    state: State<CreateAccountState>,
    innerPadding: PaddingValues,
    onAction: (RegisterAccountIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        RegisterHeroText()

        Spacer(modifier = Modifier.weight(1f))

        NicknameTextField(
            value = state.value.nickname,
            enabled = !state.value.isLoading,
            onValueChange = { onAction(RegisterAccountIntent.NicknameChanged(it)) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        EmailTextField(
            value = state.value.email,
            enabled = !state.value.isLoading,
            onValueChange = { onAction(RegisterAccountIntent.EmailChanged(it)) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        PasswordTextField(
            value = state.value.password,
            enabled = !state.value.isLoading,
            onValueChange = { onAction(RegisterAccountIntent.PasswordChanged(it)) },
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onAction(RegisterAccountIntent.RegisterAccount) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(UiConstants.SCREEN_REGISTER_BUTTON_SIGN_UP_TAG),
        ) {
            if (state.value.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = UiConstants.SCREEN_REGISTER_BUTTON_SIGN_UP,
                    modifier = Modifier.testTag(UiConstants.SCREEN_REGISTER_BUTTON_SIGN_UP),
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))
    }
}

@Composable
private fun RegisterHeroText() {
    val headlineAlpha = remember { Animatable(0f) }
    val headlineOffsetY = remember { Animatable(8f) }
    val subheadlineAlpha = remember { Animatable(0f) }
    val subheadlineOffsetY = remember { Animatable(8f) }
    val bridgeAlpha = remember { Animatable(0f) }
    val bridgeOffsetY = remember { Animatable(8f) }

    LaunchedEffect(Unit) {
        delay(600)
        launch {
            headlineAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 4000, easing = LinearOutSlowInEasing),
            )
        }
        launch {
            headlineOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 4000, easing = LinearOutSlowInEasing),
            )
        }
        delay(1000)
        launch {
            subheadlineAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 4000, easing = LinearOutSlowInEasing),
            )
        }
        launch {
            subheadlineOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 4000, easing = LinearOutSlowInEasing),
            )
        }
        delay(1200)
        launch {
            bridgeAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 4000, easing = LinearOutSlowInEasing),
            )
        }
        launch {
            bridgeOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 4000, easing = LinearOutSlowInEasing),
            )
        }
    }

    Text(
        text = stringResource(R.string.register_headline),
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            lineHeight = 32.sp,
            color = MaterialTheme.colorScheme.onSurface,
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = headlineOffsetY.value }
            .alpha(headlineAlpha.value),
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.register_subheadline),
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Light,
            letterSpacing = 0.5.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .graphicsLayer { translationY = subheadlineOffsetY.value }
            .alpha(subheadlineAlpha.value),
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = stringResource(R.string.register_bridge),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .graphicsLayer { translationY = bridgeOffsetY.value }
            .alpha(bridgeAlpha.value),
    )
}
