package com.croniot.android.presentation.devices

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.croniot.android.Global
import com.croniot.android.UiConstants
import com.croniot.android.presentation.device.sensors.ViewModelSensors
import com.croniot.android.presentation.login.LoginController
import com.croniot.android.ui.UtilUi
import com.croniot.android.ui.util.GenericAlertDialog
import croniot.models.dto.DeviceDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(navController: NavController, modifier: Modifier, devicesViewModel: DevicesViewModel, viewModelSensors: ViewModelSensors) {

    var showLogoutDialog by remember { mutableStateOf(false) }

    BackHandler {
        showLogoutDialog = true
    }

    LaunchedEffect(Unit) {
        viewModelSensors.listenToClientSensorsIfNeeded(Global.account.devices.toList())
    }

    var expanded by remember { mutableStateOf(false) }

    if(showLogoutDialog){
        GenericAlertDialog(title = "Log Out", content = "Are you sure you want to log out?"){
            val result = it
            if(result){
                LoginController.logOut(navController)
            }
            showLogoutDialog = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar( //This material API is experimental and is likely to change or to be removed in the future.
                title = {
                    Box(contentAlignment = Alignment.CenterStart) {
                        Text(text = Global.appName)
                    }
                },
                actions = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_more, ), // Triple-dot icon
                            contentDescription = "More options",
                            tint = Color.Black // Set icon color to black
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                expanded = false
                                showLogoutDialog = true
                            },
                            text = { Text("Logout") }
                        )
                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        },
        content = { innerPadding -> MainContent(
            navController,
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            devicesViewModel
        )
        }
    )
}

@Composable
fun MainContent(navController: NavController, modifier: Modifier = Modifier, devicesViewModel: DevicesViewModel) {
    val clientsState by devicesViewModel.devices.collectAsState() // Observe as state
    DevicesList(navController, clientsState, modifier) // Pass the observed list
}

@Composable
fun DevicesList(navController: NavController, items: List<DeviceDto>, modifier: Modifier) {
    Column(modifier = modifier){
        Text(text = "Devices",
            fontSize = UtilUi.TEXT_SIZE_1,
            color = Color.Black,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(
            modifier = Modifier.padding(16.dp)
        ) {

            if(items.isEmpty()){
                item{
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                        ,
                        contentAlignment = Alignment.Center
                    ){
                        Text(
                            text = "No IoT devices yet",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = UtilUi.TEXT_SIZE_2,
                            modifier = Modifier.padding(4.dp)
                            ,
                        )
                    }
                }
            }

            items(items.size) { index ->
                val client = items.elementAt(index)
                Row(
                    modifier = Modifier
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DeviceItem(navController, modifier, client)
                }
            }
        }
    }
}

@Composable
fun DeviceItem(navController: NavController, modifier: Modifier, device: DeviceDto) {
    val currentTime = remember { System.currentTimeMillis() }

    val isDeviceOnline = remember(device.lastOnlineMillis) {
        currentTime - device.lastOnlineMillis < 5000
    }

    val backgroundColor =
    if (isDeviceOnline) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable {
                Global.selectedDevice = device
                navController.navigate(UiConstants.ROUTE_DEVICE)
            },
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${device.name} ",
                    color = Color.Black,
                    fontSize = UtilUi.TEXT_SIZE_3,
                )
            }
        }
    }
}