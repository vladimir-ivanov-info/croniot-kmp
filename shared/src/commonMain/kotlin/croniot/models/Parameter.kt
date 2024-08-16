package croniot.models

import croniot.models.dto.ParameterDto
import croniot.models.dto.SensorDto
import java.util.*

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

