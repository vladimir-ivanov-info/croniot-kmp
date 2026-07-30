package com.croniot.android.app

import com.croniot.testing.fakes.FakeLocalDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val localDataRepository = FakeLocalDataRepository(deviceUuid = null)
    private lateinit var viewModel: AppViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AppViewModel(localDataRepository = localDataRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given viewModel just created, When init runs, Then generates device uuid if not exists`() = runTest {
        assertNotNull(localDataRepository.getLocalDeviceUuid())
    }

    @Test
    fun `Given a route, When onScreenChanged is called, Then saves current screen`() = runTest {
        viewModel.onScreenChanged("home")

        assertEquals("home", localDataRepository.getCurrentScreen())
    }
}
