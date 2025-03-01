package com.croniot.android.app

import com.croniot.android.domain.model.Device
import com.croniot.android.domain.model.TaskType
import java.util.*

object Global {

    val appName = "croniot"

    // TODO move to a separate util class
    fun generateUniqueString(length: Int): String {
        val uuid = UUID.randomUUID()
        val uniqueString = uuid.toString().substring(0, length)
        return uniqueString
    }

    var selectedDevice: Device? = null
    var selectedTaskType: TaskType? = null
}
