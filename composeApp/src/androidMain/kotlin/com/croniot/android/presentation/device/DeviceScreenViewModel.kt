package com.croniot.android.presentation.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class DeviceScreenViewModel : ViewModel(), KoinComponent {

    private val _currentTab = MutableStateFlow(0)
    val currentTab : StateFlow<Int> get() = _currentTab

    fun updateCurrentTab(newTab: Int){
        viewModelScope.launch {
            _currentTab.emit(newTab)
        }
    }
}