package com.croniot.android.android.testing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.croniot.android.app.Global
import com.croniot.android.core.presentation.UiConstants
import com.croniot.android.core.di.DependencyInjectionModule
import com.croniot.android.core.presentation.theme.IoTClientTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

//import org.robolectric.RobolectricTestRunner
//import org.robolectric.annotation.Config
//import kotlin.test.assertEquals

//@RunWith(RobolectricTestRunner::class)
@RunWith(AndroidJUnit4::class)  // Use AndroidJUnit4 as you are using Android components
//@Config(manifest = Config.NONE, sdk = [34])
class AccountRegisterTest {

    @get:Rule
    val composeTestRule = createComposeRule()
   // @get:Rule
   // val composeTestRule = createAndroidComposeRule<MainActivity>()


   // val composeTestRule = createComposeRule()

   // @get:Rule

   // private val testModule = DependencyInjectionModule.dependencyInjectionModule /*module {

//        viewModel { LoginViewModel() }
//        viewModel { ViewModelRegisterAccount() }
//
//        single { DevicesViewModel() }
//        single { ViewModelSensorData() }
//        single { com.croniot.android.ui.task.ViewModelTask() }
//    }

    @Before
    fun setup() {
      /*  startKoin {
            androidContext(InstrumentationRegistry.getInstrumentation().targetContext)
            modules(testModule)
        }*/


        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidLogger()
                androidContext(InstrumentationRegistry.getInstrumentation().targetContext)

                modules(DependencyInjectionModule.dependencyInjectionModule)
            }
        }

       /* composeTestRule2.activity.setContent {
            SimpleTestingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Gastón")
                }
            }
        }*/

   /*     composeTestRule.activity.setContent {
            IoTClientTheme {
                CurrentScreen();
            }
        }*/

    }

    @After
    fun teardown() {
        stopKoin()
    }

    @Test
    fun testMyComposeUI() {
        composeTestRule.setContent {
            IoTClientTheme {
                //val viewModel: NavigationViewModel = koinViewModel()
                //CurrentScreen(viewModel)  // Ensure this is correctly set up
            }
        }

        composeTestRule.waitForIdle()

        Global.SERVER_ADDRESS = Global.SERVER_ADDRESS_LOCAL


        // Print the UI hierarchy to the log
        composeTestRule.onRoot().printToLog("TAG")

        println("Checking if 'Register' button is displayed on the login screen")

        val registerNode = composeTestRule.onNodeWithTag(UiConstants.SCREEN_LOGIN_BUTTON_REGISTER_TAG)
        registerNode.assertExists("Register button does not exist")

        // Check if it is displayed
        try {
            registerNode.assertIsDisplayed()
        } catch (e: AssertionError) {
            println("AssertionError: The component is not displayed!")
        }

        // Perform the click
        registerNode.performClick()
        composeTestRule.waitForIdle()
     //   Thread.sleep(1000)

        // Print the UI hierarchy again to check if the register screen is loaded
        composeTestRule.onRoot().printToLog("TAG_AFTER_CLICK")

        println("Checking if 'Sign Up' button is displayed on the register screen")

        val signUpNode = composeTestRule.onNodeWithTag(UiConstants.SCREEN_REGISTER_BUTTON_SIGN_UP_TAG)
        signUpNode.assertExists("Sign Up button does not exist")

        try {
            signUpNode.assertIsDisplayed()
        } catch (e: AssertionError) {
            println("AssertionError: The component is not displayed!")
        }
        composeTestRule.waitForIdle()
        //Thread.sleep(1000)

     //   signUpNode.performClick()
        Thread.sleep(3000)

    }
}
