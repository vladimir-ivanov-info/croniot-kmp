package com.croniot.android.features.registeraccount.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.croniot.client.presentation.components.EmailTextField
import com.croniot.client.presentation.components.PasswordTextField
import com.croniot.client.presentation.components.UsernameTextField
import com.croniot.client.presentation.constants.UiConstants
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScreenRegisterAccountRoot(
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

    ScreenRegisterAccount(
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
fun ScreenRegisterAccount(
    state: State<CreateAccountState>,
    snackbarHostState: SnackbarHostState,
    onAction: (RegisterAccountIntent) -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                onAction(RegisterAccountIntent.NavigateBack)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        },
        content = { innerPadding ->
            ScreenRegisterAccountBody(
                state = state,
                innerPadding = innerPadding,
                modifier = Modifier,
                onAction = onAction,
            )
        },
    )
}

@Composable
fun ScreenRegisterAccountBody(
    state: State<CreateAccountState>,
    innerPadding: PaddingValues,
    modifier: Modifier,
    onAction: (RegisterAccountIntent) -> Unit,
) {
    Box(modifier = Modifier.padding(innerPadding), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = UiConstants.SCREEN_LOGIN_BUTTON_CREATE_ACCOUNT_TEXT,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 32.sp,
                )
            }

            Spacer(modifier = Modifier.padding(vertical = 4.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Warning! This is a TESTING version of the app.\n\nDo NOT use a real password that you already use somewhere else.\n\nThe connection with the server is NOT secure and the password will NOT be stored encrypted in the server's database.",
                    fontSize = 20.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(modifier = Modifier.padding(vertical = 4.dp))

            Spacer(modifier = Modifier.size(8.dp))

            UsernameTextField(
                value = state.value.nickname,
                enabled = !state.value.isLoading,
                onValueChange = {
                    onAction(RegisterAccountIntent.NicknameChanged(it))
                },
            )

            Spacer(modifier = Modifier.size(8.dp))

            EmailTextField(
                value = state.value.email,
                enabled = !state.value.isLoading,
                onValueChange = {
                    onAction(RegisterAccountIntent.EmailChanged(it))
                },
            )

            Spacer(modifier = Modifier.size(8.dp))

            PasswordTextField(
                value = state.value.password,
                enabled = !state.value.isLoading,
                onValueChange = {
                    onAction(RegisterAccountIntent.PasswordChanged(it))
                },
            )

            Spacer(modifier = Modifier.size(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        onAction(RegisterAccountIntent.RegisterAccount)
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .testTag(UiConstants.SCREEN_REGISTER_BUTTON_SIGN_UP_TAG),
                ) {
                    if (state.value.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp), // tamaño reducido para que encaje bien en el botón
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary, // color visible sobre el fondo
                        )
                    } else {
                        Text(
                            text = UiConstants.SCREEN_REGISTER_BUTTON_SIGN_UP,
                            modifier = Modifier.testTag(
                                UiConstants.SCREEN_REGISTER_BUTTON_SIGN_UP,
                            ),
                        )
                    }
                }
            }
        }
    }
}
