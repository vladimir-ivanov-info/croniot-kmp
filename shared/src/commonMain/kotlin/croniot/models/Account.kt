package croniot.models

import croniot.models.dto.AccountDto
import java.util.*

data class Account (
    var id: Long = 0,
    var uuid: String,
    var nickname: String,
    var email: String,
    var password: String,
    var devices: MutableSet<Device>
) {
   constructor() : this(0, "", "", "", "", mutableSetOf())
    constructor(uuid: String = "", nickname: String, email: String = "",  password: String = "", devices: MutableSet<Device> = mutableSetOf()) : this(0, uuid, nickname, email, password, devices)

    override fun hashCode(): Int {
        return Objects.hash(id) // or hash other relevant properties
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null || javaClass != obj.javaClass) return false
        val (id1) = obj as Account
        return id == id1 // or compare other relevant properties
    }

    override fun toString(): String {
        return "Account: $id"
    }
}

fun Account.toDto() = AccountDto(
    uuid = this.uuid,
    nickname = this.nickname,
    email = this.email,
    devices = this.devices.map { it.toDto() }.toMutableSet()
)