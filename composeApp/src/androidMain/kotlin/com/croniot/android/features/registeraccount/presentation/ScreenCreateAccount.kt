package com.croniot.android.features.registeraccount.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
        verticalArrangement = Arrangement.Center,
    ) {
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
    }
}
