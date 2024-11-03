package com.croniot.android.presentation.login

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.croniot.android.Global
import com.croniot.android.R
import com.croniot.android.SharedPreferences
import com.croniot.android.UiConstants
import com.croniot.android.ui.UtilUi
import com.croniot.android.ui.util.StatefulTextField
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)

//TODO after getting Google Maps API token

@Composable
fun ScreenMaps(navController: NavHostController) {

    Scaffold(
        topBar = {
            TopAppBar( //This material API is experimental and is likely to change or to be removed in the future.
                title = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text("Map")
                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        },
        content = { innerPadding -> LoginScreenBody2(navController, innerPadding)
            // Content goes here
        }
    )
}
@Composable
fun LoginScreenBody2(navController: NavController, innerPadding: PaddingValues){
    Box(
        Modifier
            .padding(innerPadding)
        ,
    ) {
        Login2(navController,
            Modifier
                .align(Alignment.Center)
                .padding(16.dp))
    }
}

//@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun Login2(navController: NavController, modifier: Modifier){

    Column(modifier = modifier){
        Box(modifier = Modifier
            .size(200.dp)
            .align(Alignment.CenterHorizontally)
            .clickable { //for testing
                com.croniot.android.Global.SERVER_ADDRESS = Global.SERVER_ADDRESS_REMOTE
            },
            contentAlignment = Alignment.Center, ){

        }

        //MAP

    }

}