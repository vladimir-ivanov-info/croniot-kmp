package com.croniot.android.features.configuration

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.croniot.client.presentation.components.StatefulTextField
import com.croniot.client.presentation.constants.UtilUi
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConfigurationScreenViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            modifier = Modifier.padding(end = 8.dp),
                            onClick = onNavigateBack,
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
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.align(Alignment.CenterStart),
                        text = "Run app in foreground service",
                        fontSize = UtilUi.TEXT_SIZE_4,
                    )
                    Switch(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        checked = state.foregroundServiceEnabled,
                        onCheckedChange = {
                            viewModel.onIntent(ConfigurationIntent.SetForegroundService(it))
                        },
                    )
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.align(Alignment.CenterStart),
                        text = "Use remote server",
                        fontSize = UtilUi.TEXT_SIZE_4,
                    )
                    Switch(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        checked = state.serverMode == "remote",
                        onCheckedChange = { viewModel.onIntent(ConfigurationIntent.ToggleServerMode) },
                    )
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Server IP:",
                        fontSize = UtilUi.TEXT_SIZE_4,
                    )
                    StatefulTextField(
                        value = state.serverIp,
                        placeholderString = "server IP",
                        isPassword = false,
                        onValueChange = { /* TODO */ },
                    )
                }
            }
        },
    )
}