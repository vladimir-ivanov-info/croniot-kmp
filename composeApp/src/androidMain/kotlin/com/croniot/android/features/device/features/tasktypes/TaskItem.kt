package com.croniot.android.features.device.features.tasktypes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.croniot.android.app.Global
import com.croniot.android.core.presentation.UiConstants
import com.croniot.android.core.presentation.util.UtilUi
import croniot.models.dto.TaskTypeDto

@Composable
fun TaskItem(navController: NavController, task: TaskTypeDto) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                Global.selectedTaskType = task
                navController.navigate(UiConstants.ROUTE_CREATE_TASK)
            },
    ) {
       Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            elevation = CardDefaults.elevatedCardElevation()
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = task.name,
                    fontSize = UtilUi.TEXT_SIZE_3,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .align(Alignment.Center),
                )
            }
        }
    }
}

