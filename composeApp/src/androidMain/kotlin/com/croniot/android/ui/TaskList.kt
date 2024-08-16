package com.croniot.android.ui

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
import croniot.models.dto.TaskTypeDto

@Composable
fun TaskItem(navController: NavController, task: TaskTypeDto) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                com.croniot.android.Global.selectedTaskType = task
                navController.navigate("task")
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
                        .padding(bottom = 8.dp) // Adjust the padding as needed
                        .align(Alignment.Center),
                )
            }
        }
    }
}

