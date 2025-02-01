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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.croniot.android.app.Global
import com.croniot.android.R
import com.croniot.android.core.presentation.SharedPreferencesViewModel
import com.croniot.android.core.presentation.UiConstants
import com.croniot.android.core.presentation.util.UtilUi
import com.croniot.android.core.presentation.util.StatefulTextField
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val loginViewModel: LoginViewModel = koinViewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(Global.appName, fontSize = UtilUi.TEXT_SIZE_2)
                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        },
        content = { innerPadding ->
            LoginScreenBody(
                navController = navController,
                viewModel = loginViewModel,
                innerPadding = innerPadding
            )
        }
    )
}

@Composable
fun LoginScreenBody(
    navController: NavController,
    viewModel: LoginViewModel,
    innerPadding: PaddingValues
) {

    val sharedPreferencesViewModel: SharedPreferencesViewModel = koinViewModel()

    Box(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
    ) {
        LoginContent(
            navController = navController,
            viewModel = viewModel,
            sharedPreferencesViewModel,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun LoginContent(
    navController: NavController,
    viewModel: LoginViewModel,
    sharedPreferencesViewModel: SharedPreferencesViewModel,
    modifier: Modifier
) {
    val hasNavigated = remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    if(uiState.loggedIn && !hasNavigated.value){
        hasNavigated.value = true
        navController.navigate(UiConstants.ROUTE_DEVICES) {
            popUpTo(UiConstants.ROUTE_LOGIN) { inclusive = true }
           // launchSingleTop = true
        }
    }

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo
        Box(
            modifier = Modifier
                .size(200.dp)
                .clickable { navController.navigate(UiConstants.ROUTE_CONFIGURATION) },
            contentAlignment = Alignment.Center
        ) {
            val serverMode by sharedPreferencesViewModel.serverMode.collectAsState()
            val logoResourceId = if (serverMode == "local") {
                R.drawable.logo_cockroach_test_mode
            } else {
                R.drawable.logo_cockroach
            }

            Image(
                painter = painterResource(id = logoResourceId),
                contentDescription = "App Logo",
                modifier = Modifier.clip(CircleShape),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Login",
            fontSize = UtilUi.TEXT_SIZE_1,
            modifier = Modifier
                .padding(bottom = 4.dp)
                .align(Alignment.Start),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.size(4.dp))

        StatefulTextField(
            stringFlow = viewModel.email,
            placeholderString = "Email"
        ) { viewModel.updateEmail(it) }

        Spacer(modifier = Modifier.height(16.dp))

        StatefulTextField(
            stringFlow = viewModel.password,
            placeholderString = "Password",
            isPassword = true
        ) { viewModel.updatePassword(it) }

        Spacer(modifier = Modifier.height(16.dp))

        LoginButton(navController, viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        RegisterButton(navController)
    }
}

@Composable
fun LoginButton(navController: NavController, viewModel: LoginViewModel) {
    Button(
        onClick = {
            viewModel.login()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(text = "Log in")
    }
}

@Composable
fun RegisterButton(navController: NavController) {
    Button(
        onClick = { navController.navigate(UiConstants.ROUTE_REGISTER_ACCOUNT) },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(UiConstants.SCREEN_LOGIN_BUTTON_REGISTER_TAG),
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.White,
            disabledContentColor = Color.White
        ),
    ) {
        Text(UiConstants.SCREEN_LOGIN_BUTTON_NEW_ACCOUNT_TEXT)
    }
}
