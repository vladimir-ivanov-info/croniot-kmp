package com.croniot.android.core.presentation.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.StateFlow

@Composable
fun StatefulTextField(stringFlow: StateFlow<String>, placeholderString: String, isPassword: Boolean = false, onValueChange: (newValue: String) -> Unit){

    val text by remember(stringFlow) { stringFlow }.collectAsState()

    TextField(
        value = text,
        onValueChange = { onValueChange(it) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = placeholderString) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        maxLines = 1,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
    )
}

@Composable
fun GenericAlertDialog(title: String, content: String, onResult: (result: Boolean) -> Unit){
    AlertDialog(
        onDismissRequest = {
            onResult(false)
        },
        confirmButton = {
            TextButton(onClick = { onResult(true) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = { onResult(false) }) {
                Text("Cancel")
            }
        },
        title = {
            Text(text = title)
        },
        text = {
            Text(content)
        },
        properties = DialogProperties()
    )
}
//TODO
/*
@Composable
fun FixedSizeScrollableDialog(taskToShow: TaskDto, onDismissRequest: () -> Unit) {

    var taskName by remember { mutableStateOf("") }

    val selectedDevice = com.croniot.android.app.Global.selectedDevice
    for (task in selectedDevice.tasks){
        if(task.uid == taskToShow.taskUid){
            taskName = task.name
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(400.dp),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ){
            Box(modifier = Modifier.fillMaxSize().weight(0.5F)){
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()

                ) {
                    item {
                        Text(
                            text = taskName,
                            fontSize = UtilUi.TEXT_SIZE_2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                        Divider()
                    }

                    val parametersValues = taskToShow.parametersValues
                    val parametersValuesEntries = parametersValues.entries.toList()

                    items(parametersValuesEntries.size) { index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                ,
                            ) {
                                var parameterName = ""
                                var parameterUnit = ""
                                val selectedDevice = com.croniot.android.app.Global.selectedDevice
                                for (task in selectedDevice.tasks){
                                    for(parameter in task.parameters){
                                        if(parameter.uid == parametersValuesEntries[index].key){
                                            parameterName = parameter.name
                                            parameterUnit = parameter.unit
                                        }
                                    }
                                }

                                Text(
                                    text = "$parameterName : ${parametersValuesEntries[index].value} $parameterUnit",
                                    fontSize = UtilUi.TEXT_SIZE_3,
                                    modifier = Modifier
                                        .padding(bottom = 8.dp) // Adjust the padding as needed
                                    ,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(
                    onClick = onDismissRequest
                ) {
                    Text(text = "Close")
                }
            }
        }
        }
    }
}*/

@Composable
fun GenericDialog(title: String, text: String, button1Text: String?, onButton1Clicked: () -> Unit, button2Text: String, onButton2Clicked: () -> Unit, onDismiss: () -> Unit) {

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(400.dp),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
            ){
                Box(modifier = Modifier.fillMaxSize().weight(0.5F)){
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()

                    ) {
                        item {
                            Text(
                                text = title,
                                fontSize = UtilUi.TEXT_SIZE_2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                            Divider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if(button1Text != null){
                        Button(
                            onClick = onButton1Clicked
                        ) {
                            Text(text = button1Text)
                        }
                    }
                }
            }
        }
    }
}