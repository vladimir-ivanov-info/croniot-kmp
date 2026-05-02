package com.croniot.android.features.devicelist

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.croniot.android.app.AppError
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.croniot.client.core.config.AppConfig
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.TransportKind
import com.croniot.client.core.util.getRelativeTimeText
import com.croniot.client.features.login.R
import com.croniot.client.presentation.components.GenericAlertDialog
import com.croniot.client.presentation.components.StatusDot
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun DeviceListScreen(
    onLogOut: () -> Unit,
    onDeviceClicked: (deviceUuid: String) -> Unit,
    onNavigateToBleDiscovery: () -> Unit,
    appError: AppError? = null,
    viewModel: DeviceListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* user responded, no action needed */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.reconnectIfNeeded()
    }

    LaunchedEffect(appError) {
        if (appError != null) {
            snackbarHostState.showSnackbar(
                message = "${appError.title}: ${appError.message}",
                withDismissAction = true,
                duration = SnackbarDuration.Indefinite,
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is DeviceListEffect.LogOut -> onLogOut()
                is DeviceListEffect.NavigateToDevice -> onDeviceClicked(effect.deviceUuid)
                is DeviceListEffect.NavigateToBleDiscovery -> onNavigateToBleDiscovery()
            }
        }
    }

    DeviceListScreenBody(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreenBody(
    state: DeviceListState,
    onIntent: (DeviceListIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    BackHandler { showLogoutDialog = true }

    if (showLogoutDialog) {
        GenericAlertDialog(title = "Log Out", content = "Are you sure you want to log out?") { confirmed ->
            if (confirmed) onIntent(DeviceListIntent.LogOut)
            showLogoutDialog = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_cockroach),
                                contentDescription = null,
                                modifier = Modifier.clip(CircleShape).clearAndSetSemantics { },
                                contentScale = ContentScale.Fit,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = AppConfig.appName, fontWeight = Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Actions",
                            tint = MaterialTheme.colorScheme.onSurface,
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
            DeviceListContent(
                state = state,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(MaterialTheme.colorScheme.surface),
                onIntent = onIntent,
            )
        }
    )
}

@Composable
fun DeviceListContent(
    state: DeviceListState,
    modifier: Modifier = Modifier,
    onIntent: (DeviceListIntent) -> Unit
) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1_000)
            value = System.currentTimeMillis()
        }
    }

    Column(modifier = modifier) {
        Text(
            text = "Devices",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp).semantics { heading() },
        )

        if (state.mode == TransportKind.BLE) {
            BleDiscoveryCta(
                onClick = { onIntent(DeviceListIntent.GoToBleDiscovery) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.size(12.dp))
        }

        if (state.devices.isEmpty()) {
            EmptyDeviceList()
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = state.devices, key = { it.uuid }) { device ->
                    val isOnline = when (device.transport) {
                        TransportKind.CLOUD ->
                            state.lastSeenMillis[device.uuid]?.let { ts -> (now - ts) < 5_000 } == true
                        TransportKind.BLE ->
                            device.uuid in state.inRangeUuids
                    }
                    DeviceRow(
                        device = device,
                        isOnline = isOnline,
                        lastSeen = state.lastSeenMillis[device.uuid],
                        now = now,
                        onClick = {
                            onIntent(DeviceListIntent.DeviceClicked(device.uuid))
                        },
                        onForget = if (device.transport == TransportKind.BLE) {
                            { onIntent(DeviceListIntent.ForgetBleDevice(device.uuid)) }
                        } else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun BleDiscoveryCta(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Buscar dispositivos cercanos",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Escanea y emparéjate con un nuevo ESP32",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
fun EmptyDeviceList() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_cockroach),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .alpha(0.2f),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = "No IoT devices yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DeviceRow(
    device: Device,
    isOnline: Boolean,
    lastSeen: Long?,
    now: Long,
    onClick: () -> Unit,
    onForget: (() -> Unit)? = null,
) {
    val statusText = if (isOnline) "Online" else "Offline"
    val relative = remember(lastSeen, now) { getRelativeTimeText(now, lastSeen) }
    val spoken = remember(statusText, device.name, relative) { "$statusText. ${device.name}. Última señal $relative" }

    val infoText = remember(device.sensorTypes, device.taskTypes) {
        buildList {
            if (device.sensorTypes.isNotEmpty()) add("${device.sensorTypes.size} sensor${if (device.sensorTypes.size > 1) "s" else ""}")
            if (device.taskTypes.isNotEmpty()) add("${device.taskTypes.size} task${if (device.taskTypes.size > 1) "s" else ""}")
        }.joinToString(" · ")
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                role = Role.Button
                onClick(label = "Abrir ${device.name}") {
                    onClick()
                    true
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(isOnline = isOnline)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    TransportBadge(transport = device.transport)
                }
                Text(
                    text = relative,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                if (infoText.isNotEmpty()) {
                    Text(
                        text = infoText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
            }
            if (onForget != null) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = "Más acciones para ${device.name}"
                            role = Role.Button
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Olvidar dispositivo") },
                            onClick = {
                                menuExpanded = false
                                onForget()
                            },
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TransportBadge(transport: TransportKind) {
    val (label, icon) = when (transport) {
        TransportKind.CLOUD -> "Cloud" to Icons.Default.Cloud
        TransportKind.BLE -> "BLE" to Icons.Default.Bluetooth
    }
    AssistChip(
        onClick = { },
        enabled = false,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLeadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
