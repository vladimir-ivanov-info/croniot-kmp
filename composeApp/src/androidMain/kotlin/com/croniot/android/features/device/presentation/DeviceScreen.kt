package com.croniot.android.features.device.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.croniot.android.app.AppError
import com.croniot.client.core.models.Device
import com.croniot.client.features.sensors.presentation.SensorsScreen
import com.croniot.client.features.taskhistory.presentation.TaskHistoryScreen
import com.croniot.client.features.tasktypes.presentation.tasktypes.TaskTypesScreen
import org.koin.androidx.compose.koinViewModel

val deviceScreenTabsNames = listOf("Sensors", "Task types", "Task History")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    selectedDeviceUuid: String,
    onNavigateBack: () -> Unit,
    onTaskTypeClicked: (deviceUuid: String, taskTypeUid: Long) -> Unit,
    appError: AppError? = null,
    viewModel: DeviceScreenViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.reconnectIfNeeded()
    }

    LaunchedEffect(selectedDeviceUuid) {
        viewModel.onIntent(DeviceIntent.Initialize(selectedDeviceUuid))
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

    BackHandler { onNavigateBack() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (state is DeviceState.Content) {
                        Text(text = (state as DeviceState.Content).device.name)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        },
        content = { innerPadding ->
            when (state) {
                is DeviceState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is DeviceState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = (state as DeviceState.Error).message)
                    }
                }
                is DeviceState.Content -> {
                    val content = state as DeviceState.Content
                    DeviceScreenContent(
                        device = content.device,
                        selectedTab = content.selectedTab,
                        innerPadding = innerPadding,
                        onTabSelected = { viewModel.onIntent(DeviceIntent.SelectTab(it)) },
                        onTaskTypeClicked = onTaskTypeClicked,
                    )
                }
            }
        },
    )
}

@Composable
private fun DeviceScreenContent(
    device: Device,
    selectedTab: Int,
    innerPadding: PaddingValues,
    onTabSelected: (Int) -> Unit,
    onTaskTypeClicked: (deviceUuid: String, taskTypeUid: Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            deviceScreenTabsNames.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = { Text(title) },
                )
            }
        }
        when (selectedTab) {
            0 -> SensorsScreen(device)
            1 -> TaskTypesScreen(
                selectedDevice = device,
                onTaskTypeClicked = onTaskTypeClicked,
            )
            2 -> TaskHistoryScreen(selectedDevice = device)
        }
    }
}
