package ru.sbermarket.platform

sealed class Result<E, R> {
    data class Error<E, R>(val error: E): Result<E, R>()
    data class Success<E, R>(val result: R): Result<E, R>()

    companion object {
        fun <R, E> wrap(op: () -> R, mapError: (Exception) -> E): Result<E, R> {
            return try {
                Success(op())
            } catch (e: Exception) {
                Error(mapError(e))
            }
        }

        fun <R> wrapException(op: () -> R): Result<Exception, R> {
            return try {
                Success(op())
            } catch (e: Exception) {
                Error(e)
            }
        }
    }
}
fun <E1, E2, R> Result<E1, R>.mapError(t: (E1) -> E2): Result<E2, R> {
    return when (this) {
        is Result.Error -> Result.Error(t(error))
        is Result.Success -> Result.Success(result)
    }
}


fun <E, R, R2> Result<E, R>.map(t: (R) -> R2): Result<E, R2> {
    return when (this) {
        is Result.Error -> Result.Error(error)
        is Result.Success -> Result.Success(t(result))
    }
}
