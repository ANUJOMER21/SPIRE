package com.example.spire.data.remote

import com.example.spire.core.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.NoRouteToHostException
import java.net.UnknownHostException

fun Throwable.toTransportError(): AppError = when (this) {
    is AppError -> this
    is UnknownHostException, is NoRouteToHostException -> AppError.NoInternet(this)
    is InterruptedIOException -> AppError.Timeout(this)
    is HttpException -> if (code() == 404) AppError.NotFound() else AppError.Http(code(), this)
    is SerializationException -> AppError.Parse(this)
    is IOException -> AppError.Network(this)
    else -> AppError.Unknown(this)
}

suspend inline fun <T> apiCall(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e.toTransportError())
    }
