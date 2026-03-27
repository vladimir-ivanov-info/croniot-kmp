package croniot

import kotlin.time.measureTimedValue

inline fun <T> measure(name: String, noinline log: ((String) -> Unit)? = null, block: () -> T): T {
    val result = measureTimedValue { block() }
    val message = "$name took ${result.duration}"
    if (log != null) log(message) else println(message)
    return result.value
}
