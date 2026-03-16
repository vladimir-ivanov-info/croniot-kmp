package croniot

import kotlin.time.measureTimedValue

inline fun <T> measure(name: String, block: () -> T): T {
    val result = measureTimedValue { block() }
    println("$name took ${result.duration}")
    return result.value
}
