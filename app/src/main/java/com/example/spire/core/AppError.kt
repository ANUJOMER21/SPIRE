package com.example.spire.core

sealed class AppError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NoInternet(cause: Throwable? = null) : AppError("No internet connection", cause)

    class Network(cause: Throwable? = null) : AppError("Network error", cause)
    class Timeout(cause: Throwable? = null) : AppError("The request timed out", cause)
    class Http(val code: Int, cause: Throwable? = null) : AppError("Server error ($code)", cause)
    class Parse(cause: Throwable? = null) : AppError("Unexpected response from server", cause)
    class NotFound : AppError("Not found")
    class Unknown(cause: Throwable? = null) : AppError("Something went wrong", cause)
}

fun Throwable.toAppError(): AppError = this as? AppError ?: AppError.Unknown(this)
