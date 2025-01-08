package com.croniot.android.features.configuration

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.croniot.android.core.presentation.UiConstants
import com.croniot.android.core.presentation.util.UtilUi
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(navController: NavController) {

    BackHandler {
        if(!navController.popBackStack()){
            navController.navigate(UiConstants.ROUTE_LOGIN)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar( //This material API is experimental and is likely to change or to be removed in the future.
                title = {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            modifier = Modifier.padding(end = 8.dp),
                            onClick = {
                                val result = navController.popBackStack()
                                if(!result){
                                    navController.navigate(UiConstants.ROUTE_LOGIN)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }

                        Box(contentAlignment = Alignment.CenterStart) {
                            Text(text = "Configuration")
                        }

                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        },
        content = { innerPadding -> ConfigurationScreenBody(navController, innerPadding)
            // Content goes here
        }
    )
}

@Composable
fun ConfigurationScreenBody(navController: NavController, innerPadding: PaddingValues){
    Box(
        Modifier
            .padding(innerPadding)
        ,
    ) {
        Configuration(navController,
            Modifier
                .align(Alignment.Center)
                .padding(16.dp))
    }
}

@Composable
fun Configuration(navController: NavController, modifier: Modifier){

    val context = LocalContext.current
    val configurationViewModel : ConfigurationViewModel = koinViewModel()
    val foregroundServiceEnabled by configurationViewModel.foregroundServiceEnabled.collectAsState()

    Column(modifier = modifier){
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ){
            Text(
                modifier = Modifier
                    .align(Alignment.CenterStart),
                text = "Run app in foreground service",
                fontSize = UtilUi.TEXT_SIZE_4,
            )

            Switch(
                modifier = Modifier.align(Alignment.CenterEnd),
                checked = foregroundServiceEnabled,
                onCheckedChange = {
                    configurationViewModel.setConfigurationForegoundService(context, it)
                }
            )
        }
    }
}


