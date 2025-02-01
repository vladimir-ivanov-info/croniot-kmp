package croniot.models

data class DeviceToken(
    var id: Long = 0,
    var device: Device,
    var token: String,
    // var expiryDate: DateTime
) {
    constructor() : this(0, Device(), "")
    constructor(device: Device, token: String) : this(0, device, token)

    override fun hashCode(): Int {
        var result = device.hashCode()
        result = 31 * result + token.hashCode()
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null || javaClass != obj.javaClass) return false
        val (id1) = obj as SensorType // TODO
        return id == id1 // or compare other relevant properties
    }

    override fun toString(): String {
        return "DeviceToken: $token"
    }
}
