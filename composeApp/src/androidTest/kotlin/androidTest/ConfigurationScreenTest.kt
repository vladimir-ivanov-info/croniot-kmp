package androidTest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
    fun `WHEN ConfigurationScreenBody is rendered THEN it displays the title and back button`() {
        composeTestRule.setContent {
            ConfigurationScreenBody(
                state = ConfigurationState(),
                onIntent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("Configuration").assertIsDisplayed()
        composeTestRule.onNodeWithTag("config_back_button").assertIsDisplayed()
    }

    @Test
    fun `WHEN ConfigurationScreenBody is rendered THEN it displays the server ip field`() {
        composeTestRule.setContent {
            ConfigurationScreenBody(
                state = ConfigurationState(),
                onIntent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithTag("config_server_ip_field").assertIsDisplayed()
    }

    @Test
    fun `WHEN ConfigurationScreenBody is rendered with a custom server ip THEN it displays that ip`() {
        composeTestRule.setContent {
            ConfigurationScreenBody(
                state = ConfigurationState(serverIp = "10.0.0.1"),
                onIntent = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("10.0.0.1").assertIsDisplayed()
    }

    @Test
    fun `WHEN the back button is clicked THEN it triggers navigation`() {
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
    fun `WHEN typing in the server ip field THEN it sends a SetServerIp intent`() {
        var capturedIntent: ConfigurationIntent? = null

        composeTestRule.setContent {
            ConfigurationScreenBody(
                state = ConfigurationState(serverIp = ""),
                onIntent = { capturedIntent = it },
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithTag("config_server_ip_field").performTextInput("192.168.1.50")

        val intent = capturedIntent
        assertTrue(intent is ConfigurationIntent.SetServerIp)
        assertEquals("192.168.1.50", (intent as ConfigurationIntent.SetServerIp).ip)
    }

    @Test
    fun `WHEN editing the server ip THEN it sends an updated intent on each change`() {
        val intents = mutableListOf<ConfigurationIntent>()

        composeTestRule.setContent {
            ConfigurationScreenBody(
                state = ConfigurationState(serverIp = "old"),
                onIntent = { intents.add(it) },
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithTag("config_server_ip_field").performTextInput("new")

        assertEquals(1, intents.size)
        assertTrue(intents.first() is ConfigurationIntent.SetServerIp)
        assertEquals("new", (intents.first() as ConfigurationIntent.SetServerIp).ip)
    }
}
