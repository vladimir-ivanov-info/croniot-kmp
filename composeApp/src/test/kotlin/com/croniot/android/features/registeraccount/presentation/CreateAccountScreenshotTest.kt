package com.croniot.android.features.registeraccount.presentation

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
class CreateAccountScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureCreateAccountScreen() {
        composeTestRule.setContent {
            ScreenRegisterAccountBody(
                state = mutableStateOf(CreateAccountState()),
                snackbarHostState = SnackbarHostState(),
                onAction = {},
            )
        }

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/create_account_screen.png")
    }

    @Test
    fun captureCreateAccountScreenLoading() {
        composeTestRule.setContent {
            ScreenRegisterAccountBody(
                state = mutableStateOf(CreateAccountState(isLoading = true)),
                snackbarHostState = SnackbarHostState(),
                onAction = {},
            )
        }

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/create_account_screen_loading.png")
    }
}