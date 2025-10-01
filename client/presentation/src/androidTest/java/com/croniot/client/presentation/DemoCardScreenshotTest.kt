package com.croniot.client.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import com.croniot.client.presentation.components.DemoCard
import com.karumi.shot.ScreenshotTest
import org.junit.Rule
import org.junit.Test

class DemoCardScreenshotTest : ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun demoCard_defaultLooksAsExpected() {
        composeRule.setContent {
            MaterialTheme {
                DemoCard()
                // LoginScreen()
            }
        }
        // Guarda/Compara la captura de la raíz de Compose
        compareScreenshot(composeRule, name = "demo_card_default")
    }

    @Test
    fun demoCard_customSubtitle() {
        composeRule.setContent {
            MaterialTheme {
                DemoCard(subtitle = "Status: Connected")
            }
        }
        compareScreenshot(composeRule, name = "demo_card_connected")
    }
}
