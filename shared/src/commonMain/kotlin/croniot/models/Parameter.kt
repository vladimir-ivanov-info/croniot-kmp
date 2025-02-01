package croniot.models

open class Parameter(
    var id: Long,
    var uid: Long,
    var name: String,
    var type: String,
    var unit: String,
    var description: String,
    var constraints: MutableMap<String, String>,
) {
    // Common properties and methods
}
