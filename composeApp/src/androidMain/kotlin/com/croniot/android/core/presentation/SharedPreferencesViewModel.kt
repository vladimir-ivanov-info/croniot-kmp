// package com.croniot.android.core.presentation
//
// import androidx.lifecycle.ViewModel
// import androidx.lifecycle.viewModelScope
// import com.croniot.android.core.constants.ServerConfig
// import com.croniot.android.core.data.source.local.DataStoreController
// import com.croniot.android.core.data.source.remote.retrofit.HostSelectionInterceptor
// import com.croniot.client.data.source.remote.NetworkModule
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.flow.MutableStateFlow
// import kotlinx.coroutines.flow.StateFlow
// import kotlinx.coroutines.launch
// import org.koin.core.component.KoinComponent
// import org.koin.core.component.inject
//
// // TODO turn into controller, shouldn't be a viewmodel
// class SharedPreferencesViewModel : ViewModel(), KoinComponent {
//
//    private var _serverMode = MutableStateFlow("remote")
//    val serverMode: StateFlow<String> get() = _serverMode
//
//    private val hostInterceptor: HostSelectionInterceptor by inject()
//
//    init {
//        viewModelScope.launch {
//            DataStoreController.loadData(DataStoreController.KEY_SERVER_MODE).collect { serverMode ->
//                serverMode?.let {
//                    _serverMode.value = serverMode
//
//
//
//
//                    if (serverMode == "remote") {
//                        //NetworkModule.reloadRetrofitRemote()
//                        //NetworkModule.reloadRetrofit(ServerConfig.SERVER_ADDRESS_REMOTE)
//                        hostInterceptor.host = ServerConfig.SERVER_ADDRESS_REMOTE
//                    } else {
//                        //NetworkModule.reloadRetrofitLocal()
//                        //NetworkModule.reloadRetrofit(ServerConfig.SERVER_ADDRESS_LOCAL)
//                        hostInterceptor.host = ServerConfig.SERVER_ADDRESS_LOCAL
//                    }
//                }
//            }
//        }
//    }
//
//    fun changeServerMode() {
//        viewModelScope.launch {
//            val currentServerMode = _serverMode.value
//            var newServerMode = "remote"
//
//            if (currentServerMode == "remote") {
//                newServerMode = "local"
//                ServerConfig.SERVER_ADDRESS = ServerConfig.SERVER_ADDRESS_LOCAL
//               // NetworkModule.reloadRetrofitLocal()
//                //NetworkModule.reloadRetrofit(ServerConfig.SERVER_ADDRESS_LOCAL)
//
//                hostInterceptor.host = ServerConfig.SERVER_ADDRESS_LOCAL
//            } else {
//                ServerConfig.SERVER_ADDRESS = ServerConfig.SERVER_ADDRESS_REMOTE
//               // NetworkModule.reloadRetrofit(ServerConfig.SERVER_ADDRESS_REMOTE)
//                //NetworkModule.reloadRetrofitRemote()
//
//                hostInterceptor.host = ServerConfig.SERVER_ADDRESS_REMOTE
//            }
//
//            viewModelScope.launch {
//                DataStoreController.saveData(DataStoreController.KEY_SERVER_MODE, newServerMode)
//            }
//        }
//    }
// }
