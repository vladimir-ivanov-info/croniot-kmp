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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.croniot.android.core.presentation.UiConstants
import com.croniot.android.core.presentation.util.GenericAlertDialog
import com.croniot.android.core.presentation.util.StatefulTextField
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenRegisterAccount(navController: NavController) {
    BackHandler {
        if (!navController.popBackStack()) {
            navController.navigate(UiConstants.ROUTE_LOGIN)
        }
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
                        IconButton(
                            onClick = {
                                if (!navController.popBackStack()) {
                                    navController.navigate(UiConstants.ROUTE_LOGIN)
                                }
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
        content = {
                innerPadding ->
            ScreenRegisterAccountBody(navController, innerPadding = innerPadding, Modifier)
        },
    )
}

@Composable
fun ScreenRegisterAccountBody(navController: NavController, innerPadding: PaddingValues, modifier: Modifier) {
    val viewModelRegisterAccount: ViewModelRegisterAccount = koinViewModel()
    val coroutineScope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }

    var dialogTitle by remember { mutableStateOf("") }
    var dialogContent by remember { mutableStateOf("") }

    Box(modifier = Modifier.padding(innerPadding), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = UiConstants.SCREEN_LOGIN_BUTTON_NEW_ACCOUNT_TEXT,
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

            val nicknameStateFlow = viewModelRegisterAccount.nickname
            val emailStateFlow = viewModelRegisterAccount.email
            val passwordStateFlow = viewModelRegisterAccount.password

            Spacer(modifier = Modifier.size(8.dp))
            StatefulTextField(stringFlow = nicknameStateFlow, placeholderString = "nickname") {
                viewModelRegisterAccount.updateNickname(it)
            }

            Spacer(modifier = Modifier.size(8.dp))
            StatefulTextField(stringFlow = emailStateFlow, placeholderString = "email") {
                viewModelRegisterAccount.updateEmail(it)
            }

            Spacer(modifier = Modifier.size(8.dp))
            StatefulTextField(stringFlow = passwordStateFlow, placeholderString = "password", isPassword = true) {
                viewModelRegisterAccount.updatePassword(it)
            }

            Spacer(modifier = Modifier.size(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModelRegisterAccount.registerAccount(onResult = { result ->
                                dialogTitle = "Account register"
                                dialogContent = if (result.success) {
                                    "Account registered successfully!"
                                } else {
                                    "Could not register account:\n\n ${result.message}"
                                }
                                showDialog = true
                            })
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .testTag(UiConstants.SCREEN_REGISTER_BUTTON_SIGN_UP_TAG),
                ) {
                    Text(
                        text = UiConstants.SCREEN_REGISTER_BUTTON_SIGN_UP,
                        modifier = Modifier.testTag(
                            UiConstants.SCREEN_REGISTER_BUTTON_SIGN_UP,
                        ),
                    )
                }
            }
        }
        if (showDialog) {
            GenericAlertDialog(title = dialogTitle, content = dialogContent) {
                val result = it
                if (result) {
                    if (navController.popBackStack()) {
                        navController.navigate(UiConstants.ROUTE_LOGIN)
                    }
                }
                showDialog = false
            }
        }
    }
}
