package androidTest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.croniot.android.features.configuration.ConfigurationIntent
import com.croniot.android.features.configuration.ConfigurationScreenBody
import com.croniot.android.features.configuration.ConfigurationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConfigurationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun configurationScreen_displaysAllElements() {
        composeTestRule.setContent {
            ConfigurationScreenBody(
                state = ConfigurationState(),
                onIntent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("Configuration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Run app in foreground service").assertIsDisplayed()
        composeTestRule.onNodeWithText("Use remote server").assertIsDisplayed()
        composeTestRule.onNodeWithText("Server IP:").assertIsDisplayed()
        composeTestRule.onNodeWithTag("config_back_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("config_foreground_switch").assertIsDisplayed()
        composeTestRule.onNodeWithTag("config_remote_server_switch").assertIsDisplayed()
    }

    @Test
    fun configurationScreen_foregroundSwitchOff_whenDisabled() {
        composeTestRule.setContent {
            ConfigurationScreenBody(
                state = ConfigurationState(foregroundServiceEnabled = false),
                onIntent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithTag("config_foreground_switch").assertIsOff()
    }

    @Test
    fun configurationScreen_foregroundSwitchOn_whenEnabled() {
        composeTestRule.setContent {
            ConfigurationScreenBody(
                state = ConfigurationState(foregroundServiceEnabled = true),
                onIntent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithTag("config_foreground_switch").assertIsOn()
    }

    @Test
    fun configurationScreen_remoteServerSwitchOn_whenRemote() {
        composeTestRule.setContent {
            ConfigurationScreenBody(
                state = ConfigurationState(serverMode = "remote"),
                onIntent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithTag("config_remote_server_switch").assertIsOn()
    }

    @Test
    fun configurationScreen_remoteServerSwitchOff_whenLocal() {
        composeTestRule.setContent {
            ConfigurationScreenBody(
                state = ConfigurationState(serverMode = "local"),
                onIntent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithTag("config_remote_server_switch").assertIsOff()
    }

    @Test
    fun configurationScreen_foregroundSwitchClick_sendsIntent() {
        var capturedIntent: ConfigurationIntent? = null

        composeTestRule.setContent {
            ConfigurationScreenBody(
                state = ConfigurationState(foregroundServiceEnabled = false),
                onIntent = { capturedIntent = it },
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithTag("config_foreground_switch").performClick()

        assertTrue(capturedIntent is ConfigurationIntent.SetForegroundService)
        assertEquals(true, (capturedIntent as ConfigurationIntent.SetForegroundService).enabled)
    }

    @Test
    fun configurationScreen_remoteServerSwitchClick_sendsToggleIntent() {
        var capturedIntent: ConfigurationIntent? = null

        composeTestRule.setContent {
            ConfigurationScreenBody(
                state = ConfigurationState(serverMode = "remote"),
                onIntent = { capturedIntent = it },
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithTag("config_remote_server_switch").performClick()

        assertTrue(capturedIntent is ConfigurationIntent.ToggleServerMode)
    }

    @Test
    fun configurationScreen_backButton_triggersNavigation() {
        var backPressed = false

        composeTestRule.setContent {
            ConfigurationScreenBody(
                state = ConfigurationState(),
                onIntent = {},
                onNavigateBack = { backPressed = true },
            )
        }

        composeTestRule.onNodeWithTag("config_back_button").performClick()

        assertTrue(backPressed)
    }

    @Test
    fun configurationScreen_displaysServerIp() {
        composeTestRule.setContent {
            ConfigurationScreenBody(
                state = ConfigurationState(serverIp = "10.0.0.1"),
                onIntent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("10.0.0.1").assertIsDisplayed()
    }
}