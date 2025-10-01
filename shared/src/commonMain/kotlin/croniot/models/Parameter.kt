package croniot.models

open class Parameter(
    open var id: Long,
    open var uid: Long,
    open var name: String,
    open var type: String,
    open var unit: String,
    open var description: String,
    open var constraints: MutableMap<String, String>,
) {
    // Common properties and methods
}
