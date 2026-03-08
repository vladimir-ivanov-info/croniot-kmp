package com.croniot.client.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

inline fun ViewModel.launchInVmScope(
    crossinline block: suspend CoroutineScope.() -> Unit
) {
    viewModelScope.launch {
        block()
    }
}
