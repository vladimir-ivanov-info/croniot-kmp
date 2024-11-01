package com.croniot.android.presentation.registerAccount

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.croniot.android.UiConstants
import com.croniot.android.ui.util.GenericAlertDialog
import com.croniot.android.ui.util.StatefulTextField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import croniot.models.Result
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenRegisterAccount(navController: NavController) {

    Scaffold(
        topBar = {
            TopAppBar( //This material API is experimental and is likely to change or to be removed in the future.
                title = {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                navController.popBackStack()
                            }
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
                innerPadding -> ScreenRegisterAccountBody(navController, innerPadding = innerPadding, Modifier)
        }
    )
}
//@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun ScreenRegisterAccountBody(navController: NavController, innerPadding: PaddingValues, modifier: Modifier){

    val viewModelRegisterAccount: ViewModelRegisterAccount = koinViewModel()
    val coroutineScope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogContent by remember { mutableStateOf("") }

    Box(modifier = Modifier.padding(innerPadding), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(16.dp)){
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = UiConstants.SCREEN_LOGIN_BUTTON_NEW_ACCOUNT_TEXT,
                    fontSize = 32.sp,
                )
            }

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
            StatefulTextField(stringFlow = passwordStateFlow, placeholderString = "password") {
                viewModelRegisterAccount.updatePassword(it)
            }

            Spacer(modifier = Modifier.size(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        tryRegisterAccount(coroutineScope, viewModelRegisterAccount, navController){ result ->
                            dialogTitle = "Account register"
                            if(result.success){
                                dialogContent = "Account registered successfully!"
                            } else {
                                dialogContent = "Could not register account:\n\n ${result.message}"
                            }
                            showDialog = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .testTag(UiConstants.SCREEN_REGISTER_BUTTON_SIGN_UP_TAG)
                ){
                    Text(text = UiConstants.SCREEN_REGISTER_BUTTON_SIGN_UP,  modifier = Modifier.testTag(UiConstants.SCREEN_REGISTER_BUTTON_SIGN_UP))
                }
            }
        }
        if(showDialog){
            GenericAlertDialog(dialogTitle, dialogContent){
                showDialog = it
            }
        }
    }
}

fun tryRegisterAccount(coroutineScope: CoroutineScope, viewModelRegisterAccount: ViewModelRegisterAccount, navController: NavController, onResult: (result: Result) -> Unit){
    coroutineScope.launch {
        val registerResult = viewModelRegisterAccount.registerAccount()
        onResult(registerResult)
    }
}