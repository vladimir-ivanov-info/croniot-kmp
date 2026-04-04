package com.croniot.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.croniot.android.core.presentation.theme.IoTClientTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IoTClientTheme(dynamicColor = false) {
                CurrentScreen()
            }
        }
    }
}
