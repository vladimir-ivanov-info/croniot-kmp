package com.croniot.client.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.croniot.client.presentation.constants.UtilUi

@Composable
fun GenericDialog(title: String, text: String, button1Text: String?, onButton1Clicked: () -> Unit, button2Text: String, onButton2Clicked: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(400.dp),
            elevation = CardDefaults.elevatedCardElevation(4.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize().weight(0.5F)) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth(),

                        ) {
                        item {
                            Text(
                                text = title,
                                fontSize = UtilUi.TEXT_SIZE_2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                            )
                            Divider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (button1Text != null) {
                        Button(
                            onClick = onButton1Clicked,
                        ) {
                            Text(text = button1Text)
                        }
                    }
                }
            }
        }
    }
}
