package com.croniot.client.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.croniot.client.presentation.components.DemoCard
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w480dp-h854dp-xhdpi")
class DemoCardScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun demoCard_defaultLooksAsExpected() {
        composeRule.setContent {
            MaterialTheme {
                DemoCard()
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/demo_card_default.png")
    }

    @Test
    fun demoCard_customSubtitle() {
        composeRule.setContent {
            MaterialTheme {
                DemoCard(subtitle = "Status: Connected")
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/demo_card_connected.png")
    }
}
