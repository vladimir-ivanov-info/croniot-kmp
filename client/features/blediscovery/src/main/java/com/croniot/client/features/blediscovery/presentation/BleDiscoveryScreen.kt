package com.croniot.client.features.blediscovery.presentation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.croniot.client.domain.models.ble.DiscoveredBleDevice
import com.croniot.client.domain.models.ble.KnownBleDevice
import com.croniot.client.presentation.components.PasswordField
import com.croniot.client.presentation.components.StatefulTextField
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleDiscoveryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDevice: (String) -> Unit,
    viewModel: BleDiscoveryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BleDiscoveryEffect.NavigateToDevice -> onNavigateToDevice(effect.deviceUuid)
                is BleDiscoveryEffect.ShowSnackbar -> snackbarHostState.showSnackbar(
                    message = effect.message,
                    withDismissAction = true,
                )
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.all { it }) {
            viewModel.onAction(BleDiscoveryIntent.PermissionsGranted)
        } else {
            viewModel.onAction(BleDiscoveryIntent.RefreshPermissionStatus)
        }
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        // Re-check on resume in case the user toggled permissions in Settings.
        viewModel.onAction(BleDiscoveryIntent.RefreshPermissionStatus)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Dispositivos cercanos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { innerPadding ->
        BleDiscoveryBody(
            state = state,
            innerPadding = innerPadding,
            onAction = viewModel::onAction,
            onRequestPermissions = {
                if (state.missingPermissions.isNotEmpty()) {
                    permissionLauncher.launch(state.missingPermissions.toTypedArray())
                }
            },
        )
    }

    state.pairing?.let { pairing ->
        PairDialog(
            pairing = pairing,
            onAction = viewModel::onAction,
        )
    }
}

@Composable
private fun BleDiscoveryBody(
    state: BleDiscoveryState,
    innerPadding: PaddingValues,
    onAction: (BleDiscoveryIntent) -> Unit,
    onRequestPermissions: () -> Unit,
) {
    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
        when {
            !state.permissionsGranted -> PermissionsGate(
                missing = state.missingPermissions,
                onRequest = onRequestPermissions,
            )
            else -> DeviceLists(
                state = state,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun PermissionsGate(
    missing: List<String>,
    onRequest: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Bluetooth,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Necesitamos permisos de Bluetooth para escanear dispositivos cercanos.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(8.dp))
        if (missing.isNotEmpty()) {
            Text(
                text = "Faltan: ${missing.joinToString(", ") { it.substringAfterLast('.') }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest) { Text("Conceder permisos") }
    }
}

@Composable
private fun DeviceLists(
    state: BleDiscoveryState,
    onAction: (BleDiscoveryIntent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.known.isNotEmpty()) {
            item { SectionHeader(text = "Conocidos") }
            items(state.known, key = { "k-${it.uuid}" }) { known ->
                KnownDeviceRow(
                    device = known,
                    isBusy = state.busyUuid == known.uuid,
                    onConnect = { onAction(BleDiscoveryIntent.ConnectKnown(known.uuid)) },
                    onForget = { onAction(BleDiscoveryIntent.ForgetKnown(known.uuid)) },
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
        }

        item { SectionHeader(text = "Cerca de ti") }
        if (state.nearby.isEmpty()) {
            item {
                Text(
                    text = "Buscando dispositivos…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.nearby, key = { "n-${it.uuid}" }) { discovered ->
                DiscoveredDeviceRow(
                    device = discovered,
                    onPair = {
                        onAction(BleDiscoveryIntent.PairRequested(discovered.uuid, discovered.displayName))
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun KnownDeviceRow(
    device: KnownBleDevice,
    isBusy: Boolean,
    onConnect: () -> Unit,
    onForget: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.displayName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (device.isInRange) "En rango" else "Fuera de rango",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                TextButton(
                    onClick = onConnect,
                    enabled = device.isInRange,
                ) { Text("Conectar") }
            }
            IconButton(onClick = onForget) {
                Icon(Icons.Filled.Close, contentDescription = "Olvidar")
            }
        }
    }
}

@Composable
private fun DiscoveredDeviceRow(
    device: DiscoveredBleDevice,
    onPair: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.displayName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (device.isPaired) "Ya conocido" else "RSSI ${device.rssi}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onPair, enabled = !device.isPaired) {
                Text(if (device.isPaired) "Conectado" else "Emparejar")
            }
        }
    }
}

@Composable
private fun PairDialog(
    pairing: PairingState,
    onAction: (BleDiscoveryIntent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!pairing.isSubmitting) onAction(BleDiscoveryIntent.PairDialogDismissed)
        },
        title = { Text("Emparejar ${pairing.displayName}") },
        text = {
            Column {
                StatefulTextField(
                    value = pairing.username,
                    onValueChange = { onAction(BleDiscoveryIntent.UsernameChanged(it)) },
                    label = "Usuario",
                    enabled = !pairing.isSubmitting,
                )
                Spacer(Modifier.height(8.dp))
                PasswordField(
                    value = pairing.password,
                    onValueChange = { onAction(BleDiscoveryIntent.PasswordChanged(it)) },
                    enabled = !pairing.isSubmitting,
                )
                pairing.error?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction(BleDiscoveryIntent.PairConfirmed) },
                enabled = !pairing.isSubmitting && pairing.username.isNotBlank() && pairing.password.isNotBlank(),
            ) {
                if (pairing.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Emparejar")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(BleDiscoveryIntent.PairDialogDismissed) },
                enabled = !pairing.isSubmitting,
            ) { Text("Cancelar") }
        },
    )
}
