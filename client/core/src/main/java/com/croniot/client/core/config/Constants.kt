package com.croniot.client.core.config

object Constants {
    const val DEMO_EMAIL = "croniot_demo@email.com"

    const val PARAMETER_VALUE_UNDEFINED: String = "*undefined*"

    const val ENDPOINT_ADD_TASK: String = "/api/add_task"
    const val ENDPOINT_REGISTER_ACCOUNT: String = "/api/register_account"

    const val ENDPOINT_REQUEST_TASK_STATE_INFO_SYNC: String = "/api/request_task_state_info_sync"
    const val ENDPOINT_LOGIN: String = "/api/login"
    const val ENDPOINT_TOKEN_REFRESH: String = "/api/token/refresh"
    const val ENDPOINT_LOGOUT: String = "/api/logout"
    const val ENDPOINT_TASK_CONFIGURATION: String = "/taskConfiguration/{deviceUuid}"
    const val ENDPOINT_TASK_STATE_INFO_HISTORY: String = "/taskStateInfoHistory/{deviceUuid}"
    const val ENDPOINT_TASK_STATE_INFO_HISTORY_COUNT: String = "/taskStateInfoHistoryCount/{deviceUuid}"
}
