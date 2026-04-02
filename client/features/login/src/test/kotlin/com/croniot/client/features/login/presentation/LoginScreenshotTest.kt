package com.croniot.client.features.login.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "xxhdpi")
class LoginScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureLoginScreen() {
        composeTestRule.setContent {
            LoginScreenBody(
                state = mutableStateOf(LoginState()),
                onAction = {},
                snackbarHostState = SnackbarHostState()
            )
        }

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/login_screen.png")
    }

    @Test
    fun captureLoginScreenLoading() {
        composeTestRule.setContent {
            LoginScreenBody(
                state = mutableStateOf(LoginState(isLoading = true)),
                onAction = {},
                snackbarHostState = SnackbarHostState()
            )
        }

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/login_screen_loading.png")
    }
}
