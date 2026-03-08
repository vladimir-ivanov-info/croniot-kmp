sealed class Outcome<out T, out E> {
    data class Ok<T>(val value: T) : Outcome<T, Nothing>()
    data class Err<E>(val error: E) : Outcome<Nothing, E>()
}

inline fun <T, E, R> Outcome<T, E>.map(transform: (T) -> R): Outcome<R, E> =
    when (this) {
        is Outcome.Ok -> Outcome.Ok(transform(value))
        is Outcome.Err -> this
    }

inline fun <T, E, F> Outcome<T, E>.mapError(transform: (E) -> F): Outcome<T, F> =
    when (this) {
        is Outcome.Ok -> this
        is Outcome.Err -> Outcome.Err(transform(error))
    }

inline fun <T, E, R> Outcome<T, E>.flatMap(transform: (T) -> Outcome<R, E>): Outcome<R, E> =
    when (this) {
        is Outcome.Ok -> transform(value)
        is Outcome.Err -> this
    }

inline fun <T, E> Outcome<T, E>.onSuccess(action: (T) -> Unit): Outcome<T, E> {
    if (this is Outcome.Ok) action(value)
    return this
}

inline fun <T, E> Outcome<T, E>.onFailure(action: (E) -> Unit): Outcome<T, E> {
    if (this is Outcome.Err) action(error)
    return this
}