package croniot.messages

data class MessageRegisterDevice(val accountEmail: String,
                                 val accountPassword: String,
                                 val deviceUuid: String,
                                 val deviceName: String,
                                 val deviceDescription: String)
