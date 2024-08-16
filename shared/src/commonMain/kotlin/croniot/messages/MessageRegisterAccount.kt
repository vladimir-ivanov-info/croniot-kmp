package croniot.messages

data class MessageRegisterAccount(val accountUuid: String,
                                  val nickname: String,
                                  val email: String,
                                  val password: String)