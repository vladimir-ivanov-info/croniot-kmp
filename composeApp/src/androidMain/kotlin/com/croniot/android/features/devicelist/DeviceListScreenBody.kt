package com.croniot.android.features.devicelist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.Icons
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
import com.croniot.client.core.config.AppConfig
import com.croniot.client.core.models.Device
import com.croniot.client.core.util.getRelativeTimeText
import com.croniot.client.features.login.R
import com.croniot.client.presentation.components.GenericAlertDialog
import com.croniot.client.presentation.components.StatusDot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun DeviceListScreen(
    onLogOut: () -> Unit,
    onDeviceClicked: (deviceUuid: String) -> Unit,
    viewModel: DeviceListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is DeviceListEffect.LogOut -> onLogOut()
                is DeviceListEffect.NavigateToDevice -> onDeviceClicked(effect.deviceUuid)
            }
        }
    }

    DeviceListScreenBody(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreenBody(
    state: DeviceListState,
    onIntent: (DeviceListIntent) -> Unit,
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
                            tint = Color.Black,
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        DropdownMenuItem(
                            onClick = { expanded = false; showLogoutDialog = true },
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
fun DeviceListScreenBody(
    state: DeviceListState,
    modifier: Modifier = Modifier,
    onIntent: (DeviceListIntent) -> Unit
) {
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

        if (state.devices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No IoT devices yet",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(4.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = state.devices, key = { it.uuid }) { device ->
                    val isOnline = state.lastSeenMillis[device.uuid]?.let { ts -> (now - ts) < 5_000 } == true
                    DeviceRow(
                        device = device,
                        isOnline = isOnline,
                        lastSeen = state.lastSeenMillis[device.uuid],
                        now = now,
                        onClick = {
                            onIntent(DeviceListIntent.DeviceClicked(device.uuid))
                        },
                    )
                }
            }
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
) {
    val bg by animateColorAsState(
        if (isOnline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        label = "row-bg",
    )

    val statusText = if (isOnline) "Online" else "Offline"
    val relative = remember(lastSeen, now) { getRelativeTimeText(now, lastSeen) }
    val spoken = remember(statusText, device.name, relative) { "$statusText. ${device.name}. Última señal $relative" }

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
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(isOnline = isOnline)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    text = relative,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}