package com.croniot.client.features.login.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_initialState_isCorrect() {
        val state = mutableStateOf(LoginState(email = "", password = ""))
        
        composeTestRule.setContent {
            LoginScreenBody(
                state = state,
                onAction = {},
                snackbarHostState = SnackbarHostState()
            )
        }

        composeTestRule.onNodeWithText("Log in").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_screen_login_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_screen_login_button").assertIsEnabled()
    }

    @Test
    fun loginScreen_loadingState_disablesButtons() {
        val state = mutableStateOf(LoginState(isLoading = true))
        
        composeTestRule.setContent {
            LoginScreenBody(
                state = state,
                onAction = {},
                snackbarHostState = SnackbarHostState()
            )
        }

        composeTestRule.onNodeWithTag("login_screen_login_button").assertIsNotEnabled()
    }

    @Test
    fun loginScreen_typingEmailAndPassword_updatesState() {
        var capturedAction: LoginIntent? = null
        val state = mutableStateOf(LoginState(email = "", password = ""))
        
        composeTestRule.setContent {
            LoginScreenBody(
                state = state,
                onAction = { capturedAction = it },
                snackbarHostState = SnackbarHostState()
            )
        }

        composeTestRule.onNodeWithText("e m a i l").performTextInput("test@example.com")
        assert(capturedAction is LoginIntent.EmailChanged && (capturedAction as LoginIntent.EmailChanged).value == "test@example.com")

        composeTestRule.onNodeWithText("p a s s w o r d").performTextInput("password123")
        assert(capturedAction is LoginIntent.PasswordChanged && (capturedAction as LoginIntent.PasswordChanged).value == "password123")
        
        composeTestRule.onNodeWithTag("login_screen_login_button").performClick()
        assert(capturedAction is LoginIntent.Login)
    }
}
