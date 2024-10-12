package com.croniot.android.presentation.devices

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.croniot.android.Global
import com.croniot.android.UiConstants
import com.croniot.android.ui.UtilUi
import croniot.models.dto.DeviceDto

import org.koin.java.KoinJavaComponent.get // For Java projects


private val devicesViewModel: DevicesViewModel = get(DevicesViewModel::class.java)
private val viewModelSensorData: com.croniot.android.ViewModelSensorData = get(com.croniot.android.ViewModelSensorData::class.java)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(navController: NavController) {

    LaunchedEffect(Unit) {
        viewModelSensorData.listenToClientSensors(com.croniot.android.Global.account.devices.toList())
    }

    Scaffold(
        topBar = {
            TopAppBar( //This material API is experimental and is likely to change or to be removed in the future.
                title = { Text(Global.appName) },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        },
        content = { innerPadding -> MainContent(
            navController,
            Modifier
                .padding(innerPadding)
                .fillMaxSize())
        }
    )

}

@Composable
fun MainContent(navController: NavController, modifier: Modifier = Modifier) {
    val clientsState = devicesViewModel.devices.collectAsState()
    ClientsList(navController, clientsState.value.toList(), modifier)
}

@Composable
fun ClientsList(navController: NavController, items: List<DeviceDto>, modifier: Modifier) {
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
                    GridItem(navController, modifier, client)
                }
            }
        }
    }
}

@Composable
fun GridItem(navController: NavController, modifier: Modifier, device: DeviceDto) {
    val currentTime = System.currentTimeMillis()

    val lastPingDifferenceMillis = currentTime - device.lastOnlineMillis

    val isDeviceOnline = lastPingDifferenceMillis < 5000

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