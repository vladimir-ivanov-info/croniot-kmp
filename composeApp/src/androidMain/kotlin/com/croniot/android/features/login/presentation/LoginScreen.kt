package com.croniot.android.features.login.presentation

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.croniot.android.app.Global
import com.croniot.android.R
import com.croniot.android.core.data.source.local.SharedPreferences
import com.croniot.android.core.presentation.SharedPreferencesViewModel
import com.croniot.android.core.presentation.UiConstants
import com.croniot.android.core.presentation.util.UtilUi
import com.croniot.android.core.presentation.util.StatefulTextField
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun LoginScreen(navController: NavHostController) {

    Scaffold(
        topBar = {
            TopAppBar( //This material API is experimental and is likely to change or to be removed in the future.
                title = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(Global.appName, fontSize = UtilUi.TEXT_SIZE_2)
                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        },
        content = { innerPadding -> LoginScreenBody(navController, innerPadding)
            // Content goes here
        }
    )
}
@Composable
fun LoginScreenBody(navController: NavController, innerPadding: PaddingValues){
    Box(
        Modifier
            .padding(innerPadding)
        ,
    ) {
        Login(navController,
            Modifier
                .align(Alignment.Center)
                .padding(16.dp))
    }
}

//@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun Login(navController: NavController, modifier: Modifier){


    val loginViewModel: LoginViewModel = koinViewModel()

    val isLoading by loginViewModel.isLoading.collectAsState()

    val sharedPreferencesViewModel : SharedPreferencesViewModel = koinViewModel()

    LaunchedEffect(Unit) {
        sharedPreferencesViewModel.loadCurrentServerMode()
    }

    val serverMode by sharedPreferencesViewModel.serverMode.collectAsState()

    var logoResourceId = R.drawable.logo_cockroach

    if(serverMode == "local"){
        logoResourceId = R.drawable.logo_cockroach_test_mode
    }

    if(isLoading){
        Box(Modifier.fillMaxSize()){
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
    else{
        Column(modifier = modifier){
            Box(modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
                .clickable { //for testing
                    navController.navigate(UiConstants.ROUTE_CONFIGURATION)
                },
                contentAlignment = Alignment.Center, ){
                Image(
                   // painter = painterResource(id = R.drawable.logo_cockroach),
                    painter = painterResource(id = logoResourceId),
                    contentDescription = "Example Image",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            }

            Text("Login", fontSize = UtilUi.TEXT_SIZE_1,
                modifier = Modifier.padding(bottom = 4.dp),
                 textAlign = TextAlign.Center,)
            Spacer(modifier = Modifier.size(4.dp))
            StatefulTextField(stringFlow = loginViewModel.email, placeholderString = "email") {
                loginViewModel.updateEmail(it)
            }
            Spacer(modifier = Modifier.size(16.dp))
            StatefulTextField(stringFlow = loginViewModel.password, placeholderString = "password", isPassword = true) {
                loginViewModel.updatePassword(it)
            }
            Spacer(modifier = Modifier.size(16.dp))
            LoginButton(navController, loginViewModel)
            Spacer(modifier = Modifier.size(16.dp))
            RegisterButton(navController)
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun LoginButton(navController: NavController, viewModel: LoginViewModel) {

    val coroutineScope = rememberCoroutineScope()
    var accountEmail = viewModel.email.collectAsState()
    var accountPassword = viewModel.password.collectAsState()

    val deviceUuid = SharedPreferences.loadData(SharedPreferences.KEY_DEVICE_UUID)
    val deviceToken = SharedPreferences.loadData(SharedPreferences.KEY_DEVICE_TOKEN)

    Button(
        onClick = {
            coroutineScope.launch {
                if(accountEmail.value != null && accountPassword.value != null && deviceUuid != null /*&& deviceToken != null*/) {
                    val loginResult = viewModel.tryLogin()
                    println(loginResult)
                    if(loginResult){
                        navController.navigate(UiConstants.ROUTE_DEVICES)
                    }
                } else {

                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Text(text = "Log in")
    }
}

@Composable
fun RegisterButton(navController: NavController) {

    val coroutineScope = rememberCoroutineScope()

    Button(
        onClick = {
            coroutineScope.launch {
                navController.navigate(UiConstants.ROUTE_REGISTER_ACCOUNT)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(UiConstants.SCREEN_LOGIN_BUTTON_REGISTER_TAG),
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.White,
            disabledContentColor = Color.White
        ),
    ) {
        Text(text = UiConstants.SCREEN_LOGIN_BUTTON_NEW_ACCOUNT_TEXT)
    }
}