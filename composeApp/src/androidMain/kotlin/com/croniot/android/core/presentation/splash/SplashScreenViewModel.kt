package com.croniot.android.core.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.client.core.models.auth.Outcome
import com.croniot.client.data.repositories.LocalDataRepository
import com.croniot.client.data.repositories.TasksRepository
import com.croniot.client.domain.repositories.SensorDataRepository
import com.croniot.client.domain.repositories.TaskTypesRepository
import com.croniot.client.features.login.domain.usecase.LogInUseCase
// import com.croniot.client.features.login.controller.LoginController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class SplashScreenViewModel(
    private val localDataRepository: LocalDataRepository,
    private val sensorDataRepository: SensorDataRepository,
    private val tasksRepository: TasksRepository,
    private val taskTypeRepository: TaskTypesRepository,
    private val logInUseCase: LogInUseCase,
) : ViewModel(), KoinComponent {

    private val _uiEvents = Channel<SplashScreenUiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    fun initSplash() {
        viewModelScope.launch {
            val currentAccount = localDataRepository.getCurrentAccount()
            val password = localDataRepository.getCurrentPassword()

            if (currentAccount != null && password != null) {
                val email = currentAccount.email
                val loginResult = logInUseCase(email = email, password = password)

                when (loginResult) {
                    is Outcome.Ok -> {
                        for (device in currentAccount.devices) {
                            tasksRepository.listenTasks(device.uuid)
                            tasksRepository.listenTaskStateInfos(device.uuid)

                            for (taskType in device.taskTypes) {
                                taskTypeRepository.add(device.uuid, taskType)
                            }
                        }
                    }
                    else -> Unit // TODO
                }

                /*if(loginResult.result.success){
                    for(device in currentAccount.devices){
                        tasksRepository.listenTasks(device.uuid)
                        tasksRepository.listenTaskStateInfos(device.uuid)

                        for(taskType in device.taskTypes){
                            taskTypeRepository.add(device.uuid, taskType)
                        }
                    }
                } else {
                    //TODO show UI error and log out
                }*/
            } else {
                // TODO logout !!!!
            }

            // TODO eso debe ir en el onResume
      /*  if (currentScreen != UiConstants.ROUTE_LOGIN && currentScreen != UiConstants.ROUTE_CONFIGURATION) {


            currentAccount?.let {
                for (device in currentAccount.devices) {
                    CoroutineScope(Dispatchers.IO).launch {
                        sensorDataRepository.listenToDeviceSensors(device)
                    }
                }

                CoroutineScope(Dispatchers.IO).launch {
                    // val latestLoggedInEmail = DataStoreController.loadData(DataStoreController.KEY_ACCOUNT_EMAIL).first()
                    val latestLoggedInEmail = currentAccount.email
                    val latestLoggedInPassword = localDataRepository.getCurrentPassword()

                    if (latestLoggedInEmail != null && latestLoggedInPassword != null) {
                        val result = loginUseCase.checkedLoginState(
                            latestLoggedInEmail,
                            latestLoggedInPassword,
                        ) // TODO remove parameters

                        if (!result.isSuccess) {
                            clearSessionCacheAndMoveToLoginScreen(navController)
                        }
                    } else {
                        clearSessionCacheAndMoveToLoginScreen(navController)
                    }
                }

            }
//

        }*/

            if (currentAccount != null) {
                val selectedDevice = localDataRepository.getSelectedDevice()

                for (device in currentAccount.devices) {
                    CoroutineScope(Dispatchers.IO).launch {
                        sensorDataRepository.listenToDeviceSensors(device)
                    }
                }

                if (selectedDevice != null) {
                    // Navigate to DeviceScreen
                    _uiEvents.send(
                        SplashScreenUiEvent.NavigateToDevice(selectedDevice.uuid),
                    )
                } else {
                    _uiEvents.send(
                        SplashScreenUiEvent.NavigateToDeviceList(),
                    )
                }
            } else {
                _uiEvents.send(
                    SplashScreenUiEvent.NavigateToLogin(),
                )
            }
        }
    }
}

sealed class SplashScreenUiEvent {
    class NavigateToLogin() : SplashScreenUiEvent()
    class NavigateToDeviceList : SplashScreenUiEvent()
    class NavigateToDevice(val deviceUuid: String) : SplashScreenUiEvent()
}
