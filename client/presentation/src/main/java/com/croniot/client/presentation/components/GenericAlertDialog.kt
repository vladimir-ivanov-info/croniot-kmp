package com.croniot.client.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

@Composable
fun GenericAlertDialog(title: String, content: String, onResult: (result: Boolean) -> Unit) {
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
        properties = DialogProperties(),
    )
}
