package com.croniot.android.presentation.login

import androidx.navigation.NavController
import com.croniot.android.presentation.device.sensors.ViewModelSensors
import com.croniot.android.presentation.devices.DevicesViewModel
import com.croniot.android.ui.task.ViewModelTasks
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object LoginController : KoinComponent {


    fun logOut(navController: NavController){
        val devicesViewModel: DevicesViewModel = get()
        val viewModelSensors: ViewModelSensors = get()
        val viewModelTasks: ViewModelTasks = get()

        viewModelTasks.uninit()
        devicesViewModel.uninit()
        viewModelSensors.uninit()

        navController.popBackStack()
    }

}