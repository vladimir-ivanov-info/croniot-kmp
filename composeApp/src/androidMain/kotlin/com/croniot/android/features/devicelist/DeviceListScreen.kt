package com.croniot.android.features.devicelist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.croniot.client.core.Global
import com.croniot.client.presentation.components.GenericAlertDialog
import org.koin.androidx.compose.koinViewModel
import com.croniot.client.core.models.Device
import com.croniot.client.features.login.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.surfaceColorAtElevation


@Composable
fun DeviceListScreenRoot(
    onLogOut: () -> Unit,
    onDeviceClicked: (deviceUuid: String) -> Unit,
    deviceListViewModel: DeviceListViewModel = koinViewModel(),
){

    val state = deviceListViewModel.state.collectAsStateWithLifecycle()
    val effects = deviceListViewModel.effects

    val lastSeenInfo by deviceListViewModel.lastSeenMillis.collectAsStateWithLifecycle(emptyMap())

    //val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        effects.collectLatest { effect ->
            when (effect) {
                is DeviceListEffect.LogOut -> onLogOut()
            }
        }
    }

    DeviceListScreen(
        state = state.value,
        lastSeenInfo = lastSeenInfo,
        onAction =  { action ->
            when(action){
                is DeviceListIntent.DeviceClicked -> onDeviceClicked(action.deviceUuid)
                else -> deviceListViewModel.onAction(action)
            }
        },
        //snackbarHostState = snackbarHostState
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    state: DeviceListState,
    onAction: (DeviceListIntent) -> Unit,
    lastSeenInfo: Map<String, Long?>,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    BackHandler {
        showLogoutDialog = true
    }

    var expanded by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        GenericAlertDialog(title = "Log Out", content = "Are you sure you want to log out?") {
            val result = it
            if (result) {
                onAction(DeviceListIntent.LogOut)
            }
            showLogoutDialog = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar( // This material API is experimental and is likely to change or to be removed in the future.
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ){

                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {

                            Image(
                                painter = painterResource(id = R.drawable.logo_cockroach),
                                contentDescription = null,
                                modifier = Modifier.clip(CircleShape).clearAndSetSemantics { },
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(contentAlignment = Alignment.CenterStart) {
                            Text(
                                text = Global.appName,
                                fontWeight = Bold,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            //painter = painterResource(id = android.R.drawable.ic_menu_more), // Triple-dot icon
                            contentDescription = "Actions",
                            tint = Color.Black, // Set icon color to black
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
                            text = { Text("Log out") },
                        )
                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        },
        content = { innerPadding ->
            DeviceListScreenBody(
                state = state,
                lastSeenInfo = lastSeenInfo,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                onAction = onAction
                //deviceListViewModel,

                /*onDeviceClicked = {
                    onDeviceClicked(it)
                }*/
            )
        },
    )
}

@Composable
fun DeviceListScreenBody(
    state: DeviceListState,
    modifier: Modifier = Modifier,
    lastSeenInfo: Map<String, Long?>,
    onAction: (DeviceListIntent) -> Unit,
) {

    val devices = state.devices


// Un solo ticker para TODA la lista
    val now by produceState(System.currentTimeMillis()) {
        while (true) { delay(1_000); value = System.currentTimeMillis() }
    }


    Column(modifier = modifier) {
        Text(
            text = "Devices",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp).semantics { heading() },
        )

        if(devices.isEmpty()){
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No IoT devices yet",
                    color = MaterialTheme.colorScheme.primary,
                    //fontSize = UtilUi.TEXT_SIZE_2,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(4.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {

                /*stickyHeader {
                    SectionHeader(title = "Online (${3})")
                }*/
                items(
                    items = devices,
                   //TODO key = { device -> device.uuid }
                ) { device ->
                    Row(
                        modifier = Modifier
                            //.padding(horizontal = 4.dp)
                        //.semantics { contentDescription = "Sensor ${device.name}" }
                        ,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val isOnline = lastSeenInfo[device.uuid]?.let { ts -> (now - ts) < 5_000 } == true

                     /*   DeviceItem(
                            device = device,
                            isOnline = isOnline,
                            onAction = onAction
                        )*/

                        DeviceRow(
                            device = device,
                            isOnline = isOnline,
                            lastSeen = lastSeenInfo[device.uuid],
                            now = now,
                            onClick = { onAction(DeviceListIntent.DeviceClicked(deviceUuid = device.uuid)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

/*@Composable
fun DeviceItem(
    device: Device,
    isOnline: Boolean,
    onAction: (DeviceListIntent) -> Unit
) {

    val backgroundColor by animateColorAsState(
        if (isOnline) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.secondaryContainer,
        label = "device-state-color"
    )

    val spoken = remember(device.name) {
        "Dispositivo ${device.name}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable {
                onAction(DeviceListIntent.DeviceClicked(device.uuid))
            }
            .semantics(mergeDescendants = true) {
                contentDescription = spoken              // nombre + estado
                role = Role.Button                       // se comporta como botón
                onClick(label = "Abrir ${device.name}") { // acción con etiqueta clara
                    //onDeviceClicked()
                    onAction(DeviceListIntent.DeviceClicked(device.uuid))
                    true
                }
            }
        ,
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Box(
            Modifier.fillMaxSize().background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            }
        }
    }
}*/

@Composable
fun DeviceRow(
    device: Device,
    isOnline: Boolean,
    lastSeen: Long?,
    now: Long,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (isOnline) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "row-bg"
    )

    val statusText = if (isOnline) "Online" else "Offline"
    val relative = remember(lastSeen, now) { relativeTime(now, lastSeen) }
    val spoken = "$statusText. ${device.name}. Última señal $relative"

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                role = Role.Button
                onClick(label = "Abrir ${device.name}") { onClick(); true }
            },
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            StatusDot(isOnline = isOnline)

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    // Cambia por device.type / location si lo tienes
                    text = relative,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Acción rápida: menú o botón
           /* IconButton(
                onClick = { /* onAction(DeviceListIntent.More(device.uuid)) */ },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "More actions")
            }*/
        }
    }
}

private fun relativeTime(now: Long, last: Long?): String {
    if (last == null || last <= 0L) return "Sin señal reciente"
    val diff = (now - last).coerceAtLeast(0L)
    val seconds = diff / 1000
    return when {
        seconds < 5 -> "en tiempo real"
        seconds < 60 -> "hace ${seconds}s"
        seconds < 3600 -> "hace ${seconds / 60} min"
        else -> "hace ${seconds / 3600} h"
    }
}


@Composable
private fun StatusDot(isOnline: Boolean) {
    val color by animateColorAsState(
        if (isOnline) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.error,
        label = "dot-color"
    )
    Box(
        Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(color)
            .semantics { contentDescription = if (isOnline) "Online" else "Offline" }
    )
}

